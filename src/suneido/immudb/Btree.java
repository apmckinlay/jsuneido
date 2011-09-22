/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

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

/**
 * Controls access to an append-only immutable btree.
 * <p>
 * Note: remove does not merge nodes. The tree will stay balanced in terms of nodes
 * but not necessarily in terms of keys. But the number of tree levels will never
 * shrink unless all the keys are deleted. This is based on the assumption that
 * adds are much more common than removes. Since nodes are not a fixed size
 * small nodes do not waste much space. And compacting the database will rebuild
 * btrees anyway.
 * <p>
 * The first key in tree nodes is always "nil", less than any real key.
 * @see BtreeNode, BtreeDbNode, BtreeDbMemNode, BtreeMemNode, BtreeStoreNode
 */
@NotThreadSafe
class Btree {
	protected int splitSize() { return 20; } // overridden by test
	private final Locking locking;
	private final Tran tran;
	private int root;
	private int treeLevels;
	int nnodes;
	int totalSize;
	private int modified = 0; // depends on all access via one instance

	Btree(Tran tran, Locking locking) {
		this.tran = tran;
		this.locking = locking;
		root = tran.refToInt(BtreeNode.emptyLeaf());
		treeLevels = 0;
		nnodes = 1;
	}

	Btree(Tran tran, Locking locking, BtreeInfo info) {
		this.tran = tran;
		this.locking = locking;
		this.root = info.root;
		this.treeLevels = info.treeLevels;
		this.nnodes = info.nnodes;
		this.totalSize = info.totalSize;
	}

	boolean isEmpty() {
		return treeLevels == 0 && nodeAt(0, root).isEmpty();
	}

	/**
	 * @param key A key without the final record address.
	 * @return The record address or 0 if the key wasn't found.
	 * If keys are not unique without the record address
	 * the first is returned.
	 */
	int get(Record key) {
		int adr = root;
		for (int i = 0; i < treeLevels; ++i) {
			int level = treeLevels - i;
			BtreeNode node = nodeAt(level, adr);
			Record slot = node.find(key);
			adr = getAddress(slot);
		}
		BtreeNode leaf = nodeAt(0, adr);
		Record slot = leaf.find(key);
		return slot != null && slot.startsWith(key) ? getAddress(slot) : 0;
	}

	/** Add a unique key to the btree. */
	void add(Record key) {
		boolean result = add(key, false);
		assert result == true;
	}

	/**
	 * Add a key to the btree.
	 * @return true if the key was successfully added,
	 * false if unique is true and the key already exists
	 * (ignoring the trailing data record address)
	 * */
	boolean add(Record key, boolean unique) {
		++modified;

		Record searchKey = unique ? stripAddress(key) : key;

		// search down the tree
		int adr = root;
		List<BtreeNode> treeNodes = Lists.newArrayList();
		TIntArrayList adrs = new TIntArrayList();
		for (int level = treeLevels; level > 0; --level) {
			adrs.add(adr);
			BtreeNode node = nodeAt(level, adr);
			treeNodes.add(node);
			Record slot = node.find(searchKey);
			adr = getAddress(slot);
		}

		BtreeNode leaf = nodeAt(0, adr);
		if (unique) {
			Record slot = leaf.find(searchKey);
			if (slot != null && slot.startsWith(searchKey))
				return false;
		}
		locking.writeLock(adr);

		totalSize += keySize(key);

		if (hasRoom(leaf)) {
			// normal/fast path - simply insert into leaf
			BtreeNode before = leaf;
			leaf = leaf.with(key);
			if (adr == root) {
				if (leaf != before)
					root = tran.refToInt(leaf);
			} else
				tran.redir(adr, leaf);
			return true;
		}
		// else split leaf
		Split split = leaf.split(tran, key, adr);
		++nnodes;

		// insert up the tree
		for (int i = treeNodes.size() - 1; i >= 0; --i) {
			BtreeNode treeNode = treeNodes.get(i);
			locking.writeLock(adrs.get(i));
			if (hasRoom(treeNode)) {
				treeNode = treeNode.with(split.key);
				tran.redir(adrs.get(i), treeNode);
				return true;
			}
			// else split
			split = treeNode.split(tran, split.key, adrs.get(i));
			++nnodes;
		}
		// getting here means root was split so a new root is needed
		newRoot(split);
		++nnodes;
		return true;
	}

	/** size without trailing address */
	int keySize(Record key) {
		return key.prefixSize(key.size() - 1);
	}

	private boolean hasRoom(BtreeNode node) {
		return node.size() < splitSize();
	}

	private void newRoot(Split split) {
		++treeLevels;
		root = tran.refToInt(BtreeMemNode.newRoot(tran, split));
	}

	/** used to return the results of a split */
	static class Split {
		final int level; // of node being split
		final int left;
		final int right;
		final Record key; // new value to go in parent, points to right half

		Split(int level, int left, int right, Record key) {
			this.level = level;
			this.left = left;
			this.right = right;
			this.key = key;
		}
	}

	/**
	 * Remove a key from the btree.
	 * <p>
	 * Does <u>not</u> merge nodes.
	 * Tree levels will only shrink when the <u>last</u> key is removed.
	 * @return false if the key was not found
	 */
	boolean remove(Record key) {
		++modified;

		// search down the tree
		int adr = root;
		List<BtreeNode> treeNodes = Lists.newArrayList();
		TIntArrayList adrs = new TIntArrayList();
		for (int level = treeLevels; level > 0; --level) {
			adrs.add(adr);
			BtreeNode node = nodeAt(level, adr);
			treeNodes.add(node);
			Record slot = node.find(key);
			adr = getAddress(slot);
		}

		// remove from leaf
		BtreeNode leaf = nodeAt(0, adr);
		leaf = leaf.without(key);
		locking.writeLock(adr);
		if (leaf == null)
			return false; // not found

		totalSize -= keySize(key);

		if (adr == root)
			root = tran.refToInt(leaf);
		else
			tran.redir(adr, leaf);
		if (! leaf.isEmpty() || treeLevels == 0)
			return true;	// this is the usual path
		--nnodes;

		// remove up the tree
		for (int i = treeNodes.size() - 1; i >= 0; --i) {
			BtreeNode treeNode = treeNodes.get(i);
			locking.writeLock(adrs.get(i));
			if (treeNode.size() > 1) {
				treeNode = treeNode.without(key);
				assert treeNode != null;
				tran.redir(adrs.get(i), treeNode);
				return true;
			}
			--nnodes;
		}

		// if we get to here, root node is now empty
		root = tran.refToInt(BtreeNode.emptyLeaf());
		treeLevels = 0;
		nnodes = 0;

		return true;
	}

	enum Update { OK, NOT_FOUND, ADD_FAILED };

	Update update(Record oldkey, Record newkey, boolean unique) {
		if (unique && oldkey.prefixEquals(newkey, oldkey.size() - 1))
			return updateUnique(oldkey, newkey);
		else {
			if (! remove(oldkey))
				return Update.NOT_FOUND;
			return add(newkey, unique) ? Update.OK : Update.ADD_FAILED;
		}
	}

	private Update updateUnique(Record oldkey, Record newkey) {
		assert oldkey.size() == newkey.size();

		++modified;

		// search down the tree
		int adr = root;
		List<BtreeNode> treeNodes = Lists.newArrayList();
		TIntArrayList adrs = new TIntArrayList();
		for (int level = treeLevels; level > 0; --level) {
			adrs.add(adr);
			BtreeNode node = nodeAt(level, adr);
			treeNodes.add(node);
			Record slot = node.find(oldkey);
			adr = getAddress(slot);
		}

		// update leaf
		BtreeNode leaf = nodeAt(0, adr);
		leaf = leaf.without(oldkey);
		if (leaf == null)
			return Update.NOT_FOUND;
		leaf = leaf.with(newkey);
		locking.writeLock(adr);
		tran.redir(adr, leaf);
		return Update.OK;
	}

	Iter iterator() {
		return new Iter();
	}

	Iter iterator(Record org, Record end) {
		return new Iter(org, end);
	}

	Iter iterator(Record key) {
		return new Iter(key, key);
	}

	Iter iterator(IndexIter iter) {
		return new Iter((Iter) iter);
	}

	class Iter implements IndexIter {
		private final Record from;
		private final Record to;
		// top of stack is leaf
		private final Deque<LevelInfo> stack = new ArrayDeque<LevelInfo>();
		private Record cur = null;
		private boolean rewound = true;
		private int valid;

		Iter() {
			from = MIN_RECORD;
			to = MAX_RECORD;
		}

		Iter(Record from, Record to) {
			this.from = from;
			this.to = to;
		}

		Iter(Iter iter) {
			from = iter.from;
			to = iter.to;
			cur = iter.cur;
			rewound = iter.rewound;
			valid = -1;
		}

		@Override
		public void next() {
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
				if (cur != null &&
						(cur.compareTo(from) < 0 || cur.prefixGt(to)))
					cur = null;
				if (! oldcur.equals(cur))
					return;
				// fall through
			}
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
				int adr = getAddress(slot);
				int level = info.node.level - 1;
				BtreeNode node = nodeAt(level, adr);
				stack.push(new LevelInfo(node, 0));
			}
			LevelInfo leaf = stack.peek();
			cur = leaf.node.get(leaf.pos);
			if (cur.compareTo(from) < 0 || cur.prefixGt(to))
				cur = null;
		}

		@Override
		public void prev() {
			if (rewound) {
				seek(to);
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
				if (cur != null &&
						(cur.compareTo(from) < 0 || cur.prefixGt(to)))
					cur = null;
				if (! oldcur.equals(cur))
					return;
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
				int adr = getAddress(slot);
				int level = info.node.level - 1;
				BtreeNode node = nodeAt(level, adr);
				stack.push(new LevelInfo(node, node.size() - 1));
			}
			LevelInfo leaf = stack.peek();
			cur = leaf.node.get(leaf.pos);
			if (cur.compareTo(from) < 0)
				cur = null;
		}

		/** leaves cur = null if key not found */
		private void seek(Record key) {
			valid = modified;
			stack.clear();
			cur = null;
			if (isEmpty())
				return;
			int adr = root;
			for (int level = treeLevels; level >= 0; --level) {
				BtreeNode node = nodeAt(level, adr);
				int pos = node.findPos(key);
				if (pos == -1)
					pos = node.size() - 1;
				stack.push(new LevelInfo(node, pos));
				Record slot = node.get(pos);
				if (level == 0) {
					cur = slot;
					break;
				}
				adr = getAddress(slot);
			}
		}

		@Override
		public boolean eof() {
			return rewound ? isEmpty() : cur == null;
		}

		Record cur() {
			return cur;
		}

		@Override
		public suneido.intfc.database.Record curKey() {
			return cur;
		}

		@Override
		public int keyadr() {
			return getAddress(cur);
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

	static int getAddress(Record slot) {
		return ((Number) slot.get(slot.size() - 1)).intValue();
	}

	private static Record stripAddress(Record key) {
		return new RecordBuilder().addPrefix(key, key.size() - 1).build();
	}

	BtreeNode nodeAt(int level, int adr) {
		locking.readLock(adr);
		return nodeAt(tran, level, adr);
	}

	static BtreeNode nodeAt(Tran tran, int level, int adr) {
		adr = tran.redir(adr);
		return IntRefs.isIntRef(adr)
			? (BtreeNode) tran.intToRef(adr)
			: new BtreeDbNode(level, tran.stor.buffer(adr));
	}

	void print() {
		print(new PrintWriter(System.out));
	}

	void print(Writer writer) {
		try {
			nodeAt(treeLevels, info().root).print(writer, tran, root);
			writer.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	int root() {
		return root;
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
				a.add(((long) node.level() << 32) | i);
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
		int nnodes = nodeAt(treeLevels, root).check(tran, Record.EMPTY);
		assert nnodes == this.nnodes
				: "nnodes " + this.nnodes + " but counted " + nnodes;
	}

	BtreeInfo info() {
		if (IntRefs.isIntRef(root))
			root = tran.getAdr(root);
		return new BtreeInfo(root, treeLevels, nnodes, totalSize);
	}

	public int totalSize() {
		return totalSize;
	}

	/** from is inclusive, end is exclusive */
	float rangefrac(Record from, Record to) {
		BtreeNode node = nodeAt(0, root);
		int org = node.lowerBound(from);
		int end = node.lowerBound(to);
		int n = node.size();
		if (n == 0)
			return 0;
		else if (treeLevels == 0)
			return (float) (end - org) / n;
		else {
			float pernode = (float) 1 / n;
			int fromadr = getAddress(node.find(from));
			int toadr = getAddress(node.find(to));
			float result =
					keyfracpos(toadr, to, (float) end / n, pernode) -
					keyfracpos(fromadr, from, (float) org / n, pernode);
			return result < 0 ? 0 : result;
		}
	}

	/**
	 * @param start the fraction into the index where this node starts
	 * @param nodefrac the fraction of the index under this node
	 */
	private float keyfracpos(int adr, Record key, float start, float nodefrac) {
		BtreeNode node = nodeAt(1, adr);
		assert node.size() > 0;
		int i = node.lowerBound(key);
		return start + (nodefrac * i) / node.size();
	}

}
