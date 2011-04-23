/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import gnu.trove.list.array.TLongArrayList;

import java.io.*;
import java.util.*;

import javax.annotation.concurrent.NotThreadSafe;

import suneido.util.IntArrayList;

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
 * @see BtreeNode, BtreeDbNode, BtreeMemNode
 */
@NotThreadSafe
public class Btree {
	public int maxNodeSize() { return 20; } // overridden by test
	private final Tran tran;
	private int root;
	private int treeLevels;
	int nnodes;
	private int modified = 0; // depends on all access via one instance

	public Btree(Tran tran) {
		this.tran = tran;
		root = tran.refToInt(BtreeNode.emptyLeaf());
		treeLevels = 0;
		nnodes = 1;
	}

	public Btree(Tran tran, BtreeInfo info) {
		this.tran = tran;
		this.root = info.root;
		this.treeLevels = info.treeLevels;
		this.nnodes = info.nnodes;
	}

	public boolean isEmpty() {
		return treeLevels == 0 && nodeAt(0, root).isEmpty();
	}

	/**
	 * @param key A key without the final record address.
	 * @return The record address or 0 if the key wasn't found.
	 * If keys are not unique without the record address
	 * the first is returned.
	 */
	public int get(Record key) {
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
	public void add(Record key) {
		boolean result = add(key, false);
		assert result == true;
	}

	/**
	 * Add a key to the btree.
	 * @return true if the key was successfully added,
	 * false if unique is true and the key already exists
	 * (ignoring the trailing data record address)
	 * */
	public boolean add(Record key, boolean unique) {
		++modified;

		// search down the tree
		int adr = root;
		List<BtreeNode> treeNodes = Lists.newArrayList();
		IntArrayList adrs = new IntArrayList();
		for (int level = treeLevels; level > 0; --level) {
			adrs.add(adr);
			BtreeNode node = nodeAt(level, adr);
			treeNodes.add(node);
			Record slot = node.find(key);
			adr = getAddress(slot);
		}

		BtreeNode leaf = nodeAt(0, adr);
		if (unique) {
			Record slot = leaf.find(key);
			if (slot != null && slot.prefixEquals(key, key.size() - 1))
				return false;
		}

		if (leaf.size() < maxNodeSize()) {
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
			if (treeNode.size() < maxNodeSize()) {
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
	public boolean remove(Record key) {
		++modified;

		// search down the tree
		int adr = root;
		List<BtreeNode> treeNodes = Lists.newArrayList();
		IntArrayList adrs = new IntArrayList();
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
		if (leaf == null)
			return false; // not found
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

	public boolean update(Record oldkey, Record newkey, boolean unique) {
		if (unique && oldkey.prefixEquals(newkey, oldkey.size() - 1))
			return updateUnique(oldkey, newkey);
		else {
			if (! remove(oldkey))
				return false;
			return add(newkey, unique);
		}
	}

	private boolean updateUnique(Record oldkey, Record newkey) {
		assert oldkey.size() == newkey.size();

		++modified;

		// search down the tree
		int adr = root;
		List<BtreeNode> treeNodes = Lists.newArrayList();
		IntArrayList adrs = new IntArrayList();
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
			return false; // not found
		leaf = leaf.with(newkey);
		tran.redir(adr, leaf);
		return true;
	}

	public Iter iterator() {
		return new Iter();
	}

	public class Iter {
		// top of stack is leaf
		private final Deque<LevelInfo> stack = new ArrayDeque<LevelInfo>();
		private Record cur = null;
		private boolean rewound = true;
		private int valid;

		private void first() {
			if (isEmpty())
				return;
			int adr = root;
			for (int level = treeLevels; level >= 0; --level) {
				BtreeNode node = nodeAt(level, adr);
				stack.push(new LevelInfo(node, 0));
				Record slot = node.get(0);
				if (level == 0) {
					cur = slot;
					break;
				}
				adr = getAddress(slot);
			}
			valid = modified;
		}

		private void last() {
			if (isEmpty())
				return;
			int adr = root;
			for (int level = treeLevels; level >= 0; --level) {
				BtreeNode node = nodeAt(level, adr);
				stack.push(new LevelInfo(node, node.size() - 1));
				Record slot = node.get(node.size() - 1);
				if (level == 0) {
					cur = slot;
					break;
				}
				adr = getAddress(slot);
			}
			valid = modified;
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
				stack.push(new LevelInfo(node, pos));
				Record slot = node.get(pos);
				if (level == 0) {
					cur = slot;
					break;
				}
				adr = getAddress(slot);
			}
		}

		public void next() {
			if (rewound) {
				first();
				rewound = false;
				return;
			}
			if (eof())
				return;
			if (modified != valid) {
				Record oldcur = cur;
				seek(cur);
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
		}

		public void prev() {
			if (rewound) {
				last();
				rewound = false;
				return;
			}
			if (eof())
				return;
			if (modified != valid) {
				Record oldcur = cur;
				seek(cur);
				if (! cur.equals(oldcur))
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
		}

		public boolean eof() {
			return rewound ? isEmpty() : cur == null;
		}

		public Record cur() {
			return cur;
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

	public static int getAddress(Record slot) {
		return ((Number) slot.get(slot.size() - 1)).intValue();
	}

	BtreeNode nodeAt(int level, int adr) {
		return nodeAt(tran, level, adr);
	}

	static BtreeNode nodeAt(Tran tran, int level, int adr) {
		adr = tran.redir(adr);
		return IntRefs.isIntRef(adr)
			? (BtreeNode) tran.intToRef(adr)
			: new BtreeDbNode(level, tran.stor.buffer(adr));
	}

	public void print() {
		print(new PrintWriter(System.out));
	}

	public void print(Writer writer) {
		try {
			nodeAt(treeLevels, info().root).print(writer, tran, root);
			writer.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public int root() {
		return root;
	}

	public int treeLevels() {
		return treeLevels;
	}

	public static void store(Tran tran) {
		// need to store BtreeNodes bottom up
		// sort by level without allocation
		// by packing level and intref into a long
		IntRefs intrefs = tran.intrefs;
		TLongArrayList a = new TLongArrayList();
		int i = -1;
		for (Object x : intrefs) {
			++i;
			if (x instanceof BtreeNode) {
				BtreeNode node = (BtreeNode) x;
				a.add(((long) node.level() << 32) | i);
			}
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

	public void check() {
		int nnodes = nodeAt(treeLevels, root).check(tran, Record.EMPTY);
		assert nnodes == this.nnodes
				: "nnodes " + this.nnodes + " but counted " + nnodes;
	}

	public BtreeInfo info() {
		if (IntRefs.isIntRef(root))
			root = tran.getAdr(root);
		return new BtreeInfo(root, treeLevels, nnodes);
	}

}
