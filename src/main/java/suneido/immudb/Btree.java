/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.immudb.DatabasePackage.MAX_RECORD;
import static suneido.immudb.DatabasePackage.MIN_RECORD;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;

import javax.annotation.concurrent.NotThreadSafe;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

/**
 * Append-only immutable btrees.
 * <p>
 * Note: remove does not merge nodes. The tree will stay balanced in terms of nodes
 * but not necessarily in terms of keys. But the number of tree levels will never
 * shrink unless all the keys are deleted. This is based on the assumption that
 * adds are much more common than removes. Since nodes are variable size
 * small nodes do not waste much space. And compacting the database will rebuild
 * btrees anyway.
 *
 * @see BtreeNode, BtreeDbNode, BtreeMemNode
 */
@NotThreadSafe
class Btree implements TranIndex {
	protected int splitSize() { return 20; } // overridden by tests
	final Tran tran;
	int treeLevels;
	int nnodes;
	int totalSize;
	private int modified = 0; // depends on all access via one instance
	BtreeNode rootNode;

	/** Create a new index */
	Btree(Tran tran) {
		this.tran = tran;
		rootNode = BtreeNode.emptyLeaf();
		treeLevels = 0;
		nnodes = 1;
		totalSize = 0;
	}

	/** Open an existing index */
	Btree(Tran tran, BtreeInfo info) {
		this.tran = tran;
		this.rootNode = info.rootNode;
		this.treeLevels = info.treeLevels;
		this.nnodes = info.nnodes;
		this.totalSize = info.totalSize;
		if (info.rootNode == null)
			rootNode = nodeAt(treeLevels, info.root);
	}

	boolean isEmpty() {
		return treeLevels == 0 && rootNode.isEmpty();
	}

	// get ---------------------------------------------------------------------

	/**
	 * @return The record address or 0 if the key wasn't found.
	 * If keys are not unique without the record address
	 * the first is returned.
	 */
	@Override
	public int get(Record rec) {
		return get(new BtreeKey(rec));
	}

	int get(BtreeKey key) {
		BtreeNode node = rootNode;
		for (int level = treeLevels - 1; level >= 0; --level)
			node = childNode(node, node.findPos(key));
		BtreeKey slot = node.find(key);
		return slot != null && key.key.equals(slot.key) ? slot.adr() : 0;
	}

	private BtreeNode childNode(BtreeNode node, int i) {
		return node.childNode(tran.istor, i);
	}

	// add ---------------------------------------------------------------------

	/** Add a unique key to the btree. */
	void add(BtreeKey key) {
		boolean result = add(key, true);
		assert result == true;
	}

	boolean add(BtreeKey key, boolean isKey, boolean unique) {
		return add(key, (isKey || (unique && ! key.isEmptyKey())));
	}

	/**
	 * Add a key with a trailing record address to the btree.
	 * <p>
	 * unique means keys should be unique <u>without</u> trailing data address.
	 * <p>
	 * Keys must always be unique <u>with</u> the trailing data address.
	 * <p>
	 * NOTE: unique dup check assumes that if only data address changes
	 * then old and new keys will be in same leaf node
	 * for this to work, split must set data address to MAXADR in tree keys
	 *
	 * @return true if the key was successfully added,
	 * false if unique is true and the key already exists
	 * (ignoring the trailing data record address)
	 */
	@Override
	public boolean add(BtreeKey key, boolean unique) {
		Object result = add(rootNode, key, unique);
		if (result == Boolean.FALSE)
			return false;
		else if (result instanceof BtreeNode)
			rootNode = (BtreeNode) result;
		else if (result instanceof Split)
			newRoot((Split) result);
		else
			throw new RuntimeException("unhandled result type");
		return true;
	}

	private Object add(BtreeNode node, BtreeKey key, boolean unique) {
		return node.isLeaf() ?
				addToLeaf(node, key, unique) : addToTree(node, key, unique);
	}

	private Object addToTree(BtreeNode node, BtreeKey key, boolean unique) {
		int i = node.findPos(key);
		BtreeNode child = childNode(node, i);
		Object result = add(child, key, unique); // recurse
		if (result == Boolean.FALSE)
			return result;
		else if (result instanceof BtreeNode)
			return pathCopy(node, i, (BtreeNode) result);
		else if (result instanceof Split)
			return handleSplit(node, i, (Split) result);
		else
			throw new RuntimeException("unhandled result type");
	}

	private static BtreeNode pathCopy(BtreeNode node, int i, BtreeNode child) {
		return node.withUpdate(i, child);
	}

	private Object handleSplit(BtreeNode node, int i, Split split) {
		node = node.withUpdate(i, split.left);
		return insertOrSplit(node, split.key);
	}

	/** @return updated node or BtreeSplit */
	private Object insertOrSplit(BtreeNode node, BtreeKey key) {
		if (hasRoom(node))
			// normal/fast path - simply insert
			return node.with(key);
		else {
			++nnodes;
			return split(node, key);
		}
	}

	/**
	 * @return
	 * false if duplicate,
	 * BtreeNode if inserted,
	 * BtreeSplit if split
	 */
	private Object addToLeaf(BtreeNode node, BtreeKey key, boolean unique) {
		if (! node.isEmpty()) {
			BtreeKey searchKey = unique ? new BtreeKey(key.key) : key;
			BtreeKey slot = node.find(searchKey);
			if (sameKey(unique, key, slot))
					return false; // duplicate
		}
		++modified;
		totalSize += key.keySize();
		return insertOrSplit(node, key);
	}

	private static boolean sameKey(boolean unique, BtreeKey key, BtreeKey slot) {
		if (slot == null)
			return false;
		return unique ? slot.key.equals(key.key) : slot.equals(key);
	}

	private void newRoot(Split split) {
		++nnodes;
		++treeLevels;
		BtreeKey minkey = new BtreeTreeKey(Record.EMPTY, 0, 0, split.left);
		rootNode = BtreeMemNode.from(treeLevels, minkey, split.key);
	}

	private boolean hasRoom(BtreeNode node) {
		return node.size() < splitSize();
	}

	static Split split(BtreeNode node, BtreeKey key) {
		BtreeNode left;
		BtreeNode right;
		int keyPos = node.lowerBound(key);
		if (keyPos == node.size()) {
			// key is at end of node, just make new node
			left = node;
			right = BtreeMemNode.from(node.level, key);
		} else {
			int mid = node.size() / 2;
			right = node.slice(mid, node.size());
			left = node.without(mid, node.size());
			if (keyPos <= mid)
				left = left.with(key);
			else
				right = right.with(key);
		}
		BtreeKey splitKey = node.isLeaf() ? left.last() : right.first();
		boolean max = node.isLeaf() && areUnique(left.last(), right.first());
		if (node.isTree())
			right.minimizeLeftMost();
		/*
		 * if splitting between keys that differ ignoring address
		 * then set splitKey data address to MAXADR
		 * so that if only data address changes, key will stay in same node
		 * this simplifies add duplicate check and allows optimized update
		 */
		BtreeTreeKey treeKey = max
			? new BtreeTreeKey(splitKey.key, IntRefs.MAXADR, 0, right)
			: new BtreeTreeKey(splitKey.key, splitKey.adr(), 0, right);
		return new Split(left, treeKey);
	}

	private static boolean areUnique(BtreeKey last, BtreeKey first) {
		return ! last.key.equals(first.key);
	}

	/** used to return the results of a split */
	static class Split {
		final BtreeNode left;
		final BtreeTreeKey key; // new value to go in parent, points to right half

		Split(BtreeNode left, BtreeTreeKey key) {
			this.left = left;
			this.key = key;
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.add("left", left)
					.add("key", key)
					.toString();
		}
	}

	// remove ------------------------------------------------------------------

	/**
	 * Remove a key from the btree.
	 * <p>
	 * Does <u>not</u> merge nodes.
	 * Tree levels will only shrink when the <u>last</u> key is removed.
	 * @return false if the key was not found
	 */
	@Override
	public boolean remove(BtreeKey key) {
		BtreeNode result = remove(rootNode, key);
		if (result == null)
			return false;
		totalSize -= key.keySize();
		if (result.isEmpty()) {
			rootNode = BtreeNode.emptyLeaf();
			treeLevels = 0;
			assert totalSize == 0;
			assert nnodes == 1;
			totalSize = 0;
			nnodes = 1;
		} else {
			rootNode = result;
		}
		++modified;
		return true;
	}

	/** @return null if key not found, otherwise updated node */
	private BtreeNode remove(BtreeNode node, BtreeKey key) {
		return (node.level == 0) ?
				removeFromLeaf(node, key) : removeFromTree(node, key);
	}

	private BtreeNode removeFromTree(BtreeNode node, BtreeKey key) {
		int i = node.findPos(key);
		BtreeNode child = childNode(node, i);
		BtreeNode result = remove(child, key); // recurse
		if (result == null)
			return null;
		if (result.isEmpty()) {
			--nnodes;
			node = node.without(i).minimizeLeftMost();
		} else
			node = pathCopy(node, i, result);
		return node;
	}

	private static BtreeNode removeFromLeaf(BtreeNode node, BtreeKey key) {
		return node.without(key);
	}

	@Override
	public Update update(BtreeKey oldkey, BtreeKey newkey, boolean unique) {
		if (! remove(oldkey))
			return Update.NOT_FOUND;
		return add(newkey, unique) ? Update.OK : Update.ADD_FAILED;
	}

	@Override
	public Iter iterator() {
		return new Iter();
		//return new IndexIter(new Iter());
	}

	@Override
	public Iter iterator(Record org, Record end) {
		return new Iter(org, end);
		//return new IndexIter(new Iter(org, end));
	}

	@Override
	public Iter iterator(Record key) {
		return new Iter(key, key);
		//return new IndexIter(new Iter(key, key));
	}

	@Override
	public Iter iterator(suneido.intfc.database.IndexIter iter) {
		return new Iter((Iter) iter);
		//return new IndexIter(new Iter((Iter) iter));
	}

	/**
	 * Note: Iterators "stick" when they hit eof().
	 */
	class Iter implements TranIndex.IterPlus {
		private final BtreeKey from;
		private final BtreeKey to;
		// top of stack is leaf
		private final Stack stack = new Stack();
		private BtreeKey cur;
		private boolean rewound;
		private int valid = -1;
		private BtreeKey next = null;

		private Iter() {
			this(MIN_RECORD, MAX_RECORD);
		}

		private Iter(Record from, Record to) {
			this(new BtreeKey(from), new BtreeKey(to, IntRefs.MAXADR), null, null, true);
		}

		private Iter(Iter iter) {
			this(iter.from, iter.to, iter.cur, iter.next, iter.rewound);
			assert eof() == iter.eof();
		}

		private Iter(BtreeKey from, BtreeKey to,
				BtreeKey cur, BtreeKey next, boolean rewound) {
			this.from = from;
			this.to = to;
			this.cur = cur;
			this.next = next;
			this.rewound = rewound;
		}

		@Override
		public void next() {
			next2();
			next = (cur == null) ? null : peekNext();
		}

		private void next2() {
			if (rewound) {
				rewound = false;
				seekNext(from);
				return;
			}
			if (next == null)
				cur = null;
			if (eof())
				return;
			if (isIndexModified())
				seekNext(next);
			else
				nextViaStack(); // usual fast path
		}

		private void seekNext(BtreeKey key) {
			seek(key);
			if (cur == null)
				nextViaStack();
			if (cur != null &&
					(cur.compareTo(from) < 0 || cur.compareTo(to) > 0))
				cur = null;
		}

		private void nextViaStack() {
			// advance position
			while (! stack.isEmpty() &&
					stack.peek().pos + 1 >= stack.peek().node.size())
				stack.pop();
			if (stack.isEmpty()) {
				cur = null;
				return;
			}
			++stack.peek().pos;
			while (stack.size() < treeLevels + 1) {
				LevelInfo info = stack.peek();
				BtreeNode node = childNode(info.node, info.pos);
				stack.push(new LevelInfo(node, 0));
			}
			LevelInfo leaf = stack.peek();
			cur = leaf.node.get(leaf.pos);
			if (cur.compareTo(from) < 0 || cur.compareTo(to) > 0)
				cur = null;
		}

		private BtreeKey peekNext() {
			int i = stack.size() - 1;
			while (i >= 0 &&
					stack.get(i).pos + 1 >= stack.get(i).node.size())
				--i;
			if (i < 0)
				return null;
			LevelInfo info = stack.get(i);
			int pos = info.pos + 1;
			while (i < treeLevels) {
				BtreeNode node = childNode(info.node, pos);
				info = new LevelInfo(node, 0);
				pos = 0;
				++i;
			}
			return info.node.get(pos);
		}

		@Override
		public void prev() {
			prev2();
			next = (cur == null) ? null : peekNext();
		}

		private void prev2() {
			if (rewound) {
				rewound = false;
				seek(to);
				// fall through to prevViaStack
			} else if (eof())
				return;
			if (isIndexModified())
				seek(cur);
			prevViaStack(); // usual fast path
		}

		private void prevViaStack() {
			while (! stack.isEmpty() && stack.peek().pos - 1 < 0)
				stack.pop();
			if (stack.isEmpty()) {
				cur = null;
				return;
			}
			--stack.peek().pos;
			while (stack.size() < treeLevels + 1) {
				LevelInfo info = stack.peek();
				BtreeNode node = childNode(info.node, info.pos);
				stack.push(new LevelInfo(node, node.size() - 1));
			}
			LevelInfo leaf = stack.peek();
			cur = leaf.node.get(leaf.pos);
			if (cur.compareTo(from) < 0 || cur.compareTo(to) > 0)
				cur = null;
		}

		/**
		 * seek to the first slot >= key
		 * on return, cur will be null if btree is empty
		 * or if position is past end of node
		 */
		@Override
		public void seek(BtreeKey key) {
			valid = modified;
			stack.clear();
			cur = null;
			if (isEmpty())
				return;
			BtreeNode node = rootNode;
			assert treeLevels >= 0;
			for (int level = treeLevels; ; --level) {
				int pos = node.findPos(key);
				stack.push(new LevelInfo(node, pos));
				if (level == 0) {
					cur = (pos < node.size()) ? node.get(pos) : null;
					return;
				}
				node = childNode(node, pos);
			}
		}

		@Override
		public boolean eof() {
			return cur == null;
		}

		@Override
		public Record curKey() {
			return cur.key;
		}

		@Override
		public BtreeKey cur() {
			return cur;
		}

		@Override
		public int keyadr() {
			return cur.adr();
		}

		@Override
		public void rewind() {
			cur = null;
			rewound = true;
		}

		@Override
		public BtreeKey oldNext() {
			return next;
		}

		@Override
		public boolean isIndexModified() {
			return modified != valid;
		}

		@Override
		public boolean isRewound() {
			return rewound;
		}

		public Tran tran() {
			return tran;
		}

	} // end of Iter

	private static class Stack {
		ArrayList<LevelInfo> list = Lists.newArrayList();

		void push(LevelInfo x) {
			list.add(x);
		}
		void pop() {
			list.remove(list.size() - 1);
		}
		boolean isEmpty() {
			return list.isEmpty();
		}
		int size() {
			return list.size();
		}
		LevelInfo peek() {
			return list.get(size() - 1);
		}
		// top is size() - 1, bottom is 0
		LevelInfo get(int i) {
			return list.get(i);
		}
		void clear() {
			list.clear();
		}
	}

	private static class LevelInfo {
		final BtreeNode node;
		int pos;
		LevelInfo(BtreeNode node, int pos) {
			this.node = node;
			this.pos = pos;
		}
		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
				.add("pos", pos)
				.add("node", node)
				.toString();
		}
	}

	private BtreeNode nodeAt(int level, int adr) {
		return nodeAt(tran.istor, level, adr);
	}

	static BtreeNode nodeAt(Storage stor, int level, int adr) {
		return new BtreeDbNode(level, stor.buffer(adr), adr);
	}

	void freeze() {
		rootNode.freeze();
	}

	boolean frozen() {
		return rootNode.frozen();
	}

	void print() {
		print(new PrintWriter(System.out));
	}

	void print(Writer writer) {
		try {
			writer.append("---------------------------\n");
			rootNode.print(writer, tran.istor);
			writer.append("---------------------------\n");
			writer.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	int treeLevels() {
		return treeLevels;
	}

	@Override
	public void check() {
		assert rootNode.level == treeLevels;
		int nnodes = rootNode.check(tran, new BtreeKey(Record.EMPTY), null);
		assert nnodes == this.nnodes
				: "nnodes " + this.nnodes + " but counted " + nnodes;
	}

	@Override
	public BtreeInfo info() {
		return new BtreeInfo(0, rootNode, treeLevels, nnodes, totalSize);
	}

	@Override
	public int totalSize() {
		return totalSize;
	}

	static final float MIN_FRAC = .001f;

	/** from is inclusive, end is exclusive */
	@Override
	public float rangefrac(Record from, Record to) {
		if (rootNode.size() == 0)
			return MIN_FRAC; // can this be zero? careful - subtle bugs
		boolean fromMinimal = isMinimal(from);
		boolean toMaximal = isMaximal(to);
		if (fromMinimal && toMaximal)
			return 1;
		float fromPos = fromMinimal ? 0 : fracPos(from);
		float toPos = toMaximal ? 1 : fracPos(to);
		//trace("fromPos " + fromPos + " toPos " + toPos + " = " + (toPos - fromPos));
		return Math.max(toPos - fromPos, MIN_FRAC);
	}

	private static boolean isMinimal(Record key) {
		for (int i = 0; i < key.size(); ++i)
			if (! key.getRaw(i).equals(Record.MIN_FIELD))
				return false;
		return true;
	}

	private static boolean isMaximal(Record key) {
		if (key.size() == 0)
			return false;
		for (int i = 0; i < key.size(); ++i)
			if (! key.getRaw(i).equals(Record.MAX_FIELD))
				return false;
		return true;
	}

	/** cache root childFracs */
	private float[] rootChildFracs;
	private int rootChildFracsValid;

	private float fracPos(Record rec) {
		if (rootChildFracs == null || rootChildFracsValid != modified) {
			rootChildFracs = childFracs(rootNode);
			rootChildFracsValid = modified;
		}
		float[] childFracs = rootChildFracs;
		BtreeKey key = new BtreeKey(rec);
		BtreeNode node = rootNode;
		int pos = node.findPos(key);
		float fracPos = 0;
		for (int i = 0; i < pos; ++i)
			fracPos += childFracs[i];
		//String msg = "fracPos " + node.size() + "^" + pos + " " + fracPos;

		if (treeLevels > 0) {
			float portion = childFracs[pos];
			node = childNode(node, pos);
			childFracs = childFracs(node);
			pos = node.findPos(key);
			for (int i = 0; i < pos; ++i)
				fracPos += portion * childFracs[i];
			//msg += ", " + node.size() + "^" + pos + " " + fracPos;

			if (treeLevels > 1) {
				portion *= childFracs[pos];
				node = childNode(node, pos);
				pos = node.findPos(key);
				fracPos += portion * pos / node.size();
				//msg += ", " + node.size() + "^" + pos + " " + fracPos;

				if (treeLevels > 2) {
					portion /= node.size();
					fracPos += portion * 0.5f;
					//msg += ", " + fracPos;
				}
			}
		}
		//trace(msg);
		return fracPos > 1.0 ? 1.0f : fracPos;
	}

	private float[] childFracs(BtreeNode node) {
		float total = 0;
		float[] fracs = new float[node.size()];
		for (int i = 0; i < node.size(); ++i)
			total += fracs[i] = (node.level == 0 ? 1 : childNode(node, i).size());
		//trace("childSizes " + Arrays.toString(sizes));
		for (int i = 0; i < node.size(); ++i)
			fracs[i] /= total;
		return fracs;
	}

//	void trace(String s) {
//		suneido.database.query.Table.trace(s);
//	}

}
