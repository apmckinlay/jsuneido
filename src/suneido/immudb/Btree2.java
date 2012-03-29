/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.immudb.BtreeNode.adr;
import static suneido.immudb.DatabasePackage.MAX_RECORD;
import static suneido.immudb.DatabasePackage.MIN_RECORD;
import gnu.trove.list.array.TLongArrayList;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.annotation.concurrent.NotThreadSafe;

import suneido.intfc.database.IndexIter;

import com.google.common.base.Objects;
import com.google.common.primitives.UnsignedInts;

/**
 * Append-only immutable btrees.
 * <p>
 * Note: remove does not merge nodes. The tree will stay balanced in terms of nodes
 * but not necessarily in terms of keys. But the number of tree levels will never
 * shrink unless all the keys are deleted. This is based on the assumption that
 * adds are much more common than removes. Since nodes are variable size
 * small nodes do not waste much space. And compacting the database will rebuild
 * btrees anyway.
 * <p>
 * @see BtreeNode, BtreeDbNode, BtreeMemNode
 */
@NotThreadSafe
class Btree2 implements TranIndex, Cloneable {
	protected int splitSize() { return 20; } // overridden by tests
	private final Tran tran;
	int treeLevels;
	int nnodes;
	int totalSize;
	private int modified = 0; // depends on all access via one instance
	BtreeNode rootNode;

	/** Create a new index */
	Btree2(Tran tran) {
		this.tran = tran;
		rootNode = BtreeNode.emptyLeaf();
		treeLevels = 0;
		nnodes = 1;
		totalSize = 0;
	}

	/** Open an existing index */
	Btree2(Tran tran, BtreeInfo info) {
		this.tran = tran;
		this.rootNode = info.rootNode ;
		this.treeLevels = info.treeLevels;
		this.nnodes = info.nnodes;
		this.totalSize = info.totalSize;
		if (info.rootNode == null)
			rootNode = nodeAt(treeLevels, info.root);
	}

	boolean isEmpty() {
		return treeLevels == 0 && rootNode.isEmpty();
	}

	/**
	 * @param key A key without the final record address.
	 * @return The record address or 0 if the key wasn't found.
	 * If keys are not unique without the record address
	 * the first is returned.
	 */
	@Override
	public int get(Record key) {
		BtreeNode node = rootNode;
		for (int level = treeLevels - 1; level >= 0; --level)
			node = childNode(level, node.find(key));
		Record slot = node.find(key);
		return slot != null && slot.startsWith(key) ? adr(slot) : 0;
	}

	// add ---------------------------------------------------------------------

	/** Add a unique key to the btree. */
	void add(Record key) {
		boolean result = add(key, true);
		assert result == true;
	}

	boolean add(Record key, boolean isKey, boolean unique) {
		return add(key, (isKey || (unique && ! IndexedData2.isEmptyKey(key))));
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
	 * @return true if the key was successfully added,
	 * false if unique is true and the key already exists
	 * (ignoring the trailing data record address)
	 */
	@Override
	public boolean add(Record key, boolean unique) {
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

	private Object add(BtreeNode node, Record key, boolean unique) {
		return (node.level == 0) ?
				addToLeaf(node, key, unique) : addToTree(node, key, unique);
	}

	private Object addToTree(BtreeNode node, Record key, boolean unique) {
		int i = node.findPos(key);
		BtreeNode child = childNode(node.level - 1, node.get(i));
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

	/**
	 * potential results
	 * - false = duplicate
	 * - BtreeNode = inserted
	 * - BtreeSplit = split
	 */
	private Object addToLeaf(BtreeNode node, Record key, boolean unique) {
		if (! node.isEmpty()) {
			Record searchKey = unique ? withoutAddress(key) : key;
			Record slot = node.find(searchKey);
			if (slot != null && slot.startsWith(searchKey))
				return false; // duplicate
		}
		++modified;
		totalSize += keySize(key);
		return insertOrSplit(node, key);
	}

	/** @return updated node or BtreeSplit */
	private Object insertOrSplit(BtreeNode node, Record key) {
		if (hasRoom(node))
			// normal/fast path - simply insert
			return node.with(key);
		else {
			++nnodes;
			return split(node, key);
		}
	}

	private void newRoot(Split split) {
		++nnodes;
		++treeLevels;
		Record minkey = BtreeNode.minimalKey(split.key.size(), split.left);
		rootNode = BtreeMemNode.from(treeLevels, minkey, split.key);
	}

	/** size without trailing address */
	int keySize(Record key) {
		return key.prefixSize(key.size() - 1);
	}

	private boolean hasRoom(BtreeNode node) {
		return node.size() < splitSize();
	}

	static Split split(BtreeNode node, Record key) {
		BtreeNode left;
		BtreeNode right;
		int keyPos = node.lowerBound(key);
		boolean leftSame = keyPos == node.size();
		if (leftSame) {
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
		Record splitKey = node.isLeaf() ? left.last() : right.first();
		boolean max = node.isLeaf() && splitMaxAdr(left.last(), right.first());
		if (node.isTree())
			right.minimizeLeftMost();
		int splitKeySize = splitKey.size();
		if (node.isTree())
			--splitKeySize;
		/*
		 * if unique, set splitKey data address to MAXADR
		 * so that if only data address changes, key will stay in same node
		 * this simplifies add duplicate check and allows optimized update
		 */
		splitKey = max
			? new RecordBuilder().addPrefix(splitKey, splitKeySize - 1)
					.adduint(IntRefs.MAXADR).addRef(right).build()
			: new RecordBuilder().addPrefix(splitKey, splitKeySize)
					.addRef(right).build();
		return new Split(left, splitKey);
	}

	private static boolean splitMaxAdr(Record last, Record first) {
		return ! last.prefixEquals(first, first.size() - 1);
	}

	/** used to return the results of a split */
	private static class Split {
		final BtreeNode left;
		final Record key; // new value to go in parent, points to right half

		Split(BtreeNode left, Record key) {
			this.left = left;
			this.key = key;
		}

		@Override
		public String toString() {
			return Objects.toStringHelper(this)
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
	public boolean remove(Record key) {
		BtreeNode result = remove(rootNode, key);
		if (result == null)
			return false;
		totalSize -= keySize(key);
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
	private BtreeNode remove(BtreeNode node, Record key) {
		return (node.level == 0) ?
				removeFromLeaf(node, key) : removeFromTree(node, key);
	}

	private BtreeNode removeFromTree(BtreeNode node, Record key) {
		int i = node.findPos(key);
		BtreeNode child = childNode(node.level - 1, node.get(i));
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

	private static BtreeNode removeFromLeaf(BtreeNode node, Record key) {
		return node.without(key);
	}

	@Override
	public Update update(Record oldkey, Record newkey, boolean unique) {
		if (! remove(oldkey))
			return Update.NOT_FOUND;
		return add(newkey, unique) ? Update.OK : Update.ADD_FAILED;
	}

	@Override
	public Iter iterator() {
		return new Iter();
	}

	@Override
	public Iter iterator(Record org, Record end) {
		return new Iter(org, end);
	}

	@Override
	public Iter iterator(Record key) {
		return new Iter(key, key);
	}

	@Override
	public Iter iterator(IndexIter iter) {
		return new Iter((Iter) iter);
	}

	/**
	 * Note: Iterators "stick" when they hit eof().
	 */
	class Iter implements TranIndex.Iter {
		private static final int UINT_MAX = 0xffffffff;
		private final Record from;
		private final Record to;
		// top of stack is leaf
		private final Deque<LevelInfo> stack = new ArrayDeque<LevelInfo>();
		private Record cur;
		private boolean rewound;
		private int valid = -1;
		private int prevAdr;

		private Iter() {
			this(MIN_RECORD, MAX_RECORD);
		}

		private Iter(Record from, Record to) {
			this(from, to, null, true, UINT_MAX);
		}

		private Iter(Iter iter) {
			this(iter.from, iter.to, iter.cur, iter.rewound, iter.prevAdr);
		}

		private Iter(Record from, Record to, Record cur, boolean rewound, int prevAdr) {
			this.from = from;
			this.to = to;
			this.cur = cur;
			this.rewound = rewound;
			this.prevAdr = prevAdr;
		}

		@Override
		public void next() {
			int prevAdr = this.prevAdr;
			this.prevAdr = tran.intrefs.next();
			if (rewound) {
				seek(from);
				rewound = false;
				if (cur != null) {
					if (cur.compareTo(from) < 0 || cur.prefixGt(to))
						cur = null;
					return;
				}
			} else if (eof())
				return;
			if (modified != valid) {
				Record oldcur = cur;
				seek(cur);
				if (cur != null) {
					if (cur.compareTo(from) < 0 || cur.prefixGt(to)) {
						cur = null;
						return;
					}
					if (! oldcur.equals(cur) &&
							UnsignedInts.compare(adr(cur), prevAdr) < 0)
						return; // already on the next key
				}
				// fall through
			}
			advance();
		}

		private void advance() {
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
				Record slot = info.node.get(info.pos);
				int level = info.node.level - 1;
				BtreeNode node = childNode(level, slot);
				stack.push(new LevelInfo(node, 0));
			}
			LevelInfo leaf = stack.peek();
			cur = leaf.node.get(leaf.pos);
			if (cur.compareTo(from) < 0 || cur.prefixGt(to))
				cur = null;
		}

		@Override
		public void prev() {
			int prevAdr = this.prevAdr;
			this.prevAdr = tran.intrefs.next();
			if (rewound) {
				seek(to);
				rewound = false;
				if (cur != null) {
					if (cur.compareTo(from) < 0) {
						cur = null;
						return;
					}
					if (! cur.prefixGt(to))
						return;
				}
			} else if (eof())
				return;
			if (modified != valid) {
				Record oldcur = cur;
				seek(cur);
				if (cur == null && ! stack.isEmpty()) {
					--stack.peek().pos;
					LevelInfo leaf = stack.peek();
					cur = leaf.node.get(leaf.pos);
				}
				if (cur != null) {
					if (cur.compareTo(from) < 0) {
						cur = null;
						return;
					}
					if (! cur.prefixGt(to) && ! oldcur.equals(cur) &&
							UnsignedInts.compare(adr(cur), prevAdr) < 0)
						return;
				}
				// fall through
			}
			while (! stack.isEmpty() &&
					stack.peek().pos - 1 < 0)
				stack.pop();
			if (stack.isEmpty()) {
				cur = null;
				return;
			}
			--stack.peek().pos;
			while (stack.size() < treeLevels + 1) {
				LevelInfo info = stack.peek();
				Record slot = info.node.get(info.pos);
				int level = info.node.level - 1;
				BtreeNode node = childNode(level, slot);
				stack.push(new LevelInfo(node, node.size() - 1));
			}
			LevelInfo leaf = stack.peek();
			cur = leaf.node.get(leaf.pos);
			if (cur.compareTo(from) < 0 || cur.prefixGt(to))
				cur = null;
		}

		/**
		 * seek to the first slot >= key
		 * on return, cur will be null if btree is empty
		 * or if position is past end of node
		 */
		private void seek(Record key) {
			valid = modified;
			stack.clear();
			cur = null;
			if (isEmpty())
				return;
			BtreeNode node = rootNode;
			for (int level = treeLevels; level >= 0; --level) {
				int pos = node.findPos(key);
				stack.push(new LevelInfo(node, pos));
				Record slot = pos < node.size() ? node.get(pos) : null;
				if (level == 0) {
					cur = slot;
					break;
				}
				node = childNode(level - 1, slot);
			}
		}

		@Override
		public boolean eof() {
			return cur == null;
		}

		@Override
		public Record curKey() {
			return cur;
		}

		@Override
		public int keyadr() {
			return adr(cur);
		}

		@Override
		public void rewind() {
			cur = null;
			rewound = true;
			prevAdr = UINT_MAX;
		}

	}

	private static class LevelInfo {
		BtreeNode node;
		int pos;
		LevelInfo(BtreeNode node, int pos) {
			this.node = node;
			this.pos = pos;
		}
		@Override
		public String toString() {
			return Objects.toStringHelper(this)
				.add("pos", pos)
				.add("node", node)
				.toString();
		}
	}

	static Record withoutAddress(Record key) {
		return new RecordPrefix(key, key.size() - 1);
	}

	BtreeNode childNode(int level, Record key) {
		assert level >= 0;
		Object child = key.childRef();
		return (child != null) ? (BtreeNode) child : nodeAt(level, adr(key));
	}

	static BtreeNode childNode(Storage stor, int level, Record key) {
		Object child = key.childRef();
		return (child != null) ? (BtreeNode) child : nodeAt(stor, level, adr(key));
	}

	BtreeNode nodeAt(int level, int adr) {
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
			rootNode.print2(writer, tran.istor);
			writer.append("---------------------------\n");
			writer.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	int treeLevels() {
		return treeLevels;
	}

	static void store(Tran tran) {
		// need to store BtreeNodes bottom up
		// sort by level without allocation
		// by packing level and intref into a long
		IntRefs intrefs = tran.intrefs;
		TLongArrayList a = new TLongArrayList();
		int i = 0;
		for (Object x : intrefs) {
			if (x instanceof BtreeNode) {
				BtreeNode node = (BtreeNode) x;
				a.add(((long) node.level << 32) | i);
			}
			++i;
		}
		a.sort();
		for (i = 0; i < a.size(); ++i) {
			long n = a.get(i);
			int intref = (int) n | IntRefs.MASK;
			BtreeNode node = (BtreeNode) intrefs.intToRef(intref);
			int adr = node.store(tran);
			if (adr != 0)
				tran.setAdr(intref, adr);
		}
	}

	@Override
	public void check() {
		assert rootNode.level == treeLevels;
		int nnodes = rootNode.check2(tran, Record.EMPTY, null);
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

	/** from is inclusive, end is exclusive */
	@Override
	public float rangefrac(Record from, Record to) {
		BtreeNode node = rootNode;
		int n = node.size();
		if (n == 0)
			return 0;
		int org = node.lowerBound(from);
		int end = node.lowerBound(to);
		if (treeLevels == 0)
			return (float) (end - org) / n;
		else {
			float pernode = (float) 1 / n;
			BtreeNode fromNode = childNode(1, node.find(from));
			BtreeNode toNode = childNode(1, node.find(to));
			float result =
					keyfracpos(toNode, to, (float) end / n, pernode) -
					keyfracpos(fromNode, from, (float) org / n, pernode);
			return result < 0 ? 0 : result;
		}
	}

	/**
	 * @param start the fraction into the index where this node starts
	 * @param nodefrac the fraction of the index under this node
	 */
	private static float keyfracpos(BtreeNode node, Record key, float start, float nodefrac) {
		assert node.size() > 0;
		int i = node.lowerBound(key);
		return start + (nodefrac * i) / node.size();
	}

}
