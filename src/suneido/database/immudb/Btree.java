/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.io.*;
import java.util.Arrays;
import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;

import suneido.util.IntArrayList;

import com.google.common.collect.Lists;

// TODO iteration

/**
 * Controls access to an append-only immutable btree.
 * <p>
 * Note: If a key is unique without it's data address
 * then it can be updated via redirection
 * otherwise it must be updated by delete and insert
 * since it's position may change, potentially to a different node.
 * @see BtreeNode, BtreeDbNode, BtreeMemNode
 */
@NotThreadSafe
public class Btree {
	public int maxNodeSize() { return 20; } // overridden by test
	private final Tran tran;
	private int root;
	private int treeLevels = 0;

	public Btree(Tran tran) {
		this.tran = tran;
		root = tran.refToInt(BtreeMemNode.emptyLeaf());
		treeLevels = 0;
	}

	public Btree(Tran tran, int root, int treeLevels) {
		this.tran = tran;
		this.root = root;
		this.treeLevels = treeLevels;
	}

	/**
	 * @param key A key without the final record address.
	 * @return The record address or 0 if the key wasn't found.
	 */
	public int get(Record key) {
		int adr = root;
		for (int i = 0; i < treeLevels; ++i) {
			int level = treeLevels - i;
			BtreeNode node = nodeAt(level, adr);
			Record slot = node.find(key);
			if (slot == null)
				return 0; // not found
			adr = getAddress(slot);
		}
		BtreeNode leaf = nodeAt(0, adr);
		Record slot = leaf.find(key);
		return slot != null && slot.startsWith(key) ? getAddress(slot) : 0;
	}

	public void add(Record key) {
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
		if (leaf.size() < maxNodeSize()) {
			// normal/fast path - simply insert into leaf
			BtreeNode before = leaf;
			leaf = leaf.with(key);
			if (adr == root) {
				if (leaf != before)
					root = tran.refToInt(leaf);
			} else
				tran.redir(adr, leaf);
			return;
		}
		// else split leaf
		Split split = leaf.split(tran, key, adr);

		// insert up the tree
		for (int i = treeNodes.size() - 1; i >= 0; --i) {
			BtreeNode treeNode = treeNodes.get(i);
			if (treeNode.size() < maxNodeSize()) {
				treeNode = treeNode.with(split.key);
				if (adrs.get(i) == root)
					root = tran.refToInt(treeNode);
				else
					tran.redir(adrs.get(i), treeNode);
				return;
			}
			// else split
			split = treeNode.split(tran, split.key, adrs.get(i));
		}
		// getting here means root was split so a new root is needed
		newRoot(split);
	}

	public boolean remove(Record key) {
		// search down the tree
		int adr = root;
		List<BtreeNode> treeNodes = Lists.newArrayList();
		IntArrayList adrs = new IntArrayList();
		for (int level = treeLevels; level > 0; --level) {
			adrs.add(adr);
			BtreeNode node = nodeAt(level, adr);
			treeNodes.add(node);
			Record slot = node.find(key);
			if (slot == null)
				return false; // not found
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

		// remove up the tree
		for (int i = treeNodes.size() - 1; i >= 0; --i) {
			BtreeNode treeNode = treeNodes.get(i);
			treeNode = treeNode.without(key);
			assert treeNode != null;
			if (adrs.get(i) == root)
				root = tran.refToInt(treeNode);
			else
				tran.redir(adrs.get(i), treeNode);
			if (i > 0 && ! treeNode.isEmpty())
				return true;
		}

		// remove root nodes with only a single key
		for (; treeLevels > 0; --treeLevels) {
			BtreeNode node = nodeAt(treeLevels, root);
			if (node.size() > 1)
				return true;
			root = getAddress(node.get(0));
		}

		return true;
	}

	public static int getAddress(Record slot) {
		return ((Number) slot.get(slot.size() - 1)).intValue();
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

	BtreeNode nodeAt(int level, int adr) {
		return nodeAt(tran, level, adr);
	}

	static BtreeNode nodeAt(Tran tran, int level, int adr) {
		adr = tran.redir(adr);
		return IntRefs.isIntRef(adr)
			? (BtreeMemNode) tran.intToRef(adr)
			: new BtreeDbNode(level, tran.stor.buffer(adr));
	}

	public void print() {
		print(new PrintWriter(System.out));
	}

	public void print(Writer writer) {
		try {
			nodeAt(treeLevels, root).print(writer, tran);
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

	public void store() {
		// need to store BtreeNodes bottom up
		// sort by level without allocation
		// by packing level and intref into a long
		IntRefs intrefs = tran.intrefs;
		long a[] = new long[intrefs.size()];
		int i = -1;
		for (Object x : intrefs) {
			++i;
			if (x instanceof BtreeNode) {
				BtreeNode node = (BtreeNode) x;
				a[i] = ((long) node.level() << 32) | i;
			}
		}
		Arrays.sort(a);
		for (long n : a) {
			int intref = (int) n | IntRefs.MASK;
			BtreeNode node = (BtreeNode) intrefs.intToRef(intref);
			int adr = node.store(tran);
			if (adr != 0)
				tran.setAdr(intref, adr);
		}
		if (IntRefs.isIntRef(root))
			root = tran.getAdr(root);
	}

}
