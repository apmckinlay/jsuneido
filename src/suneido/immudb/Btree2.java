/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.immudb.BtreeNode.adr;
import static suneido.immudb.DatabasePackage.MAX_RECORD;
import static suneido.immudb.DatabasePackage.MIN_RECORD;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import suneido.intfc.database.IndexIter;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
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

	/** Add a unique key to the btree. */
	void add(Record key) {
		boolean result = add(key, true);
		assert result == true;
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
		// search down the tree
		BtreeNode node = rootNode;
		List<BtreeNode> treeNodes = Lists.newArrayList();
		TIntArrayList idxs = new TIntArrayList();
		for (int level = treeLevels - 1; level >= 0; --level) {
			treeNodes.add(node);
			int i = node.findPos(key);
			idxs.add(i);
			node = childNode(level - 1, node.get(i));
		}

		if (! node.isEmpty()) {
			Record searchKey = unique ? withoutAddress(key) : key;
			Record slot = node.find(searchKey);
			if (slot != null && slot.startsWith(searchKey))
				return false; // duplicate
		}
		totalSize += keySize(key);

		assert treeNodes.size() == treeLevels;
		int level = treeLevels - 1;
		if (hasRoom(node)) {
			// normal/fast path - simply insert into leaf
			node = insert(node, key);
			if (node == null)
				return true;
		} else {
			BtreeSplit2 split = BtreeSplit2.split(node, key);
			++nnodes;
			// insert up the tree, continue while nodes are full and must be split
			for (; level >= 0; --level) {
				node = treeNodes.get(level);
				if (hasRoom(node)) {
					node = insert(node, split.key);
					if (node == null)
						return true;
					--level;
					break;
				}
				// else split
				split = BtreeSplit2.split(node, split.key);
				++nnodes;
			}
			if (level == -1) {
				// root was split so create a new root
				newRoot(split);
				return true;
			}
		}

		pathCopy(treeNodes, idxs, level, node);
		return true;
	}

	/** @return null if the node was root or already mutable and we're all done */
	private BtreeNode insert(BtreeNode node, Record key) {
		BtreeNode before = node;
		node = node.with(key);
		if (treeLevels == 0)
			rootNode = node;
		return (treeLevels == 0 || node == before) ? null : node;
	}

	private void newRoot(BtreeSplit2 split) {
		++nnodes;
		++treeLevels;
		rootNode = BtreeMemNode.newRoot(split);
	}

	/** size without trailing address */
	int keySize(Record key) {
		return key.prefixSize(key.size() - 1);
	}

	private boolean hasRoom(BtreeNode node) {
		return node.size() < splitSize();
	}

	/**
	 * Remove a key from the btree.
	 * <p>
	 * Does <u>not</u> merge nodes.
	 * Tree levels will only shrink when the <u>last</u> key is removed.
	 * @return false if the key was not found
	 */
	@Override
	public boolean remove(Record key) {
		++modified;

		// search down the tree
		BtreeNode node = rootNode;
		List<BtreeNode> treeNodes = Lists.newArrayList();
		TIntArrayList idxs = new TIntArrayList();
		for (int level = treeLevels - 1; level >= 0; --level) {
			treeNodes.add(node);
			int i = node.findPos(key);
			idxs.add(i);
			node = childNode(level - 1, node.get(i));
		}

		// remove from leaf
		BtreeNode before = node;
		node = node.without(key);
		if (node == null)
			return false; // not found
		totalSize -= keySize(key);
		if (treeLevels == 0)
			rootNode = node;
		if (treeLevels == 0 || (node == before && ! node.isEmpty()))
			return true; // all done


		int level = treeLevels - 1;
		if (node.isEmpty()) {
			// remove up the tree, continue while resulting nodes are empty
			--nnodes;
			for (; level >= 0; --level) {
				node = treeNodes.get(level);
				// if node only has one key don't bother updating it to empty
				// since we're going to remove the pointer to it anyway
				if (node.size() > 1) {
					before = node;
					node = node.without(node.findPos(key));
					node.minimizeLeftMost();
					if (node == before)
						return true; // mutable so pathCopy not required
					else {
						--level;
						break;
					}
				}
				--nnodes;
			}
			if (level == -1) {
				// root node is now empty
				rootNode = BtreeNode.emptyLeaf();
				treeLevels = 0;
				nnodes = 1;
				return true;
			}
		}

		pathCopy(treeNodes, idxs, level, node);
		return true;
	}

	// path copy the rest of the way up, stopping if we hit a mutable node
	private void pathCopy(List<BtreeNode> treeNodes, TIntArrayList idxs,
			int level, BtreeNode node) {
		for (int i = level; i >= 0; --i) {
			BtreeNode child = node;
			node = treeNodes.get(i);
			BtreeNode before = node;
			node = node.withUpdate(idxs.get(i), child);
			if (node == before) // node was already mutable
				break;
		}
		rootNode = node;
	}

	/*
	 * NOTE: updateUnique assumes that if only data address changes
	 * then old and new keys will be in same leaf node.
	 * For this to work, split must set data address to MAXADR in tree keys
	 */
	@Override
	public Update update(Record oldkey, Record newkey, boolean unique) {
		/*if (unique && oldkey.prefixEquals(newkey, oldkey.size() - 1))
			return updateUnique(oldkey, newkey);
		else*/ {
			if (! remove(oldkey))
				return Update.NOT_FOUND;
			return add(newkey, unique) ? Update.OK : Update.ADD_FAILED;
		}
	}

//	private Update updateUnique(Record oldkey, Record newkey) {
//		assert oldkey.size() == newkey.size();
//
//		++modified;
//
//		// search down the tree
//		int adr = root;
//		List<BtreeNode> treeNodes = Lists.newArrayList();
//		TIntArrayList adrs = new TIntArrayList();
//		for (int level = treeLevels; level > 0; --level) {
//			adrs.add(adr);
//			BtreeNode node = nodeAt(level, adr);
//			treeNodes.add(node);
//			Record slot = node.find(oldkey);
//			adr = adr(slot);
//		}
//
//		// update leaf
//		BtreeNode leaf = nodeAt(0, adr);
//		leaf = leaf.without(oldkey);
//		if (leaf == null)
//			return Update.NOT_FOUND;
//		leaf = leaf.with(newkey);
//		tran.redir(adr, leaf);
//		return Update.OK;
//	}

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

	private static Record withoutAddress(Record key) {
		return new RecordPrefix(key, key.size() - 1);
	}

	BtreeNode childNode(int level, Record key) {
		Object child = key.childRef();
		return (child != null) ? (BtreeNode) child : nodeAt(level, adr(key));
	}

	static BtreeNode childNode(Tran tran, int level, Record key) {
		Object child = key.childRef();
		return (child != null) ? (BtreeNode) child : nodeAt(tran, level, adr(key));
	}

	BtreeNode nodeAt(int level, int adr) {
		return nodeAt(tran, level, adr);
	}

	static BtreeNode nodeAt(Tran tran, int level, int adr) {
		return new BtreeDbNode(level, tran.stor.buffer(adr), adr);
	}

	//TODO squeeze
	void freeze() {
		rootNode.freeze();
	}

	void print() {
		print(new PrintWriter(System.out));
	}

	void print(Writer writer) {
		try {
			writer.append("---------------------------\n");
			rootNode.print2(writer, tran);
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

	void check() {
		int nnodes = rootNode.check2(tran, Record.EMPTY);
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
