/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.io.*;

/**
 * An append-only immutable btree.
 * @see BtreeNode, BtreeLeafNode, BtreeTreeNode
 */
public class Btree {
	public static final int MAX_NODE_SIZE = 20;
	private static final int MAX_LEVELS = 20;
	private final Tran tran;
	private int root = 0; // an IntRef
	private int treeLevels = 0;

	public Btree(Tran tran) {
		this.tran = tran;
		root = tran.refToInt(BtreeMemNode.emptyLeaf(tran));
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
		BtreeNode treeNodes[] = new BtreeNode[MAX_LEVELS];
		int i;
		for (i = 0; i < treeLevels; ++i) {
			treeNodes[i] = treeNodeAt(adr);
			Record slot = treeNodes[i].find(key);
			adr = getAddress(slot);
		}
		BtreeNode leaf = leafNodeAt(adr);
		Record slot = leaf.find(key);
		return slot != null && slot.startsWith(key) ? getAddress(slot) : 0;
	}

	public void add(Record key) {
		// search down the tree
		int adr = root;
		BtreeNode treeNodes[] = new BtreeNode[MAX_LEVELS];
		int adrs[] = new int[MAX_LEVELS];
		int i;
		for (i = 0; i < treeLevels; ++i) {
			adrs[i] = adr;
			treeNodes[i] = treeNodeAt(adr);
			Record slot = treeNodes[i].find(key);
			adr = getAddress(slot);
		}

		BtreeNode leaf = leafNodeAt(adr);
		if (leaf.size() < MAX_NODE_SIZE) {
			// normal/fast path - simply insert into leaf
			leaf = leaf.with(tran, key);
			if (adr == root)
				root = tran.refToInt(leaf);
			else
				tran.redir(adr, leaf);
			return;
		}
		// else split leaf
		Split split = leaf.split(tran, key, adr);

		// insert up the tree
		for (--i; i >= 0; --i) {
			BtreeNode tree = treeNodes[i];
			if (tree.size() < MAX_NODE_SIZE) {
				tree = tree.with(tran, split.key);
				if (adrs[i] == root)
					root = tran.refToInt(tree);
				else
					tran.redir(adrs[i], tree);
				return;
			}
			// else split
			split = tree.split(tran, split.key, adrs[i]);
		}
		// getting here means root was split so a new root is needed
		root = newRoot(split);
	}

	public static int getAddress(Record slot) {
		return (int)((Number) slot.get(slot.size() - 1)).longValue();
	}

	private int newRoot(Split split) {
		++treeLevels;
		BtreeNode node = BtreeMemNode.newRoot(tran, split);
		return tran.refToInt(node);
	}

	/** used to return the results of a split */
	static class Split {
		int left;
		int right;
		Record key; // new value to go in parent, points to right half

		Split(int left, int right, Record key) {
			this.left = left;
			this.right = right;
			this.key = key;
		}
	}

	BtreeNode treeNodeAt(int adr) {
		return treeNodeAt(tran, adr);
	}

	static BtreeNode treeNodeAt(Tran tran, int adr) {
		adr = tran.redir(adr);
		return IntRefs.isIntRef(adr)
			? (BtreeMemNode) tran.intToRef(adr)
			: BtreeDbNode.tree(tran.stor.buffer(adr));
	}

	BtreeNode leafNodeAt(int adr) {
		return leafNodeAt(tran, adr);
	}

	static BtreeNode leafNodeAt(Tran tran, int adr) {
		adr = tran.redir(adr);
		return IntRefs.isIntRef(adr)
			? (BtreeMemNode) tran.intToRef(adr)
			: BtreeDbNode.leaf(tran.stor.buffer(adr));
	}

	public void print() {
		print(new PrintWriter(System.out));
	}

	public void print(Writer writer) {
		try {
			BtreeNodeMethods.print(writer, tran, rootNode(), treeLevels);
			writer.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private BtreeNode rootNode() {
		return treeLevels == 0 ? leafNodeAt(root) : treeNodeAt(root);
	}

	public int root() {
		return root;
	}

	public int treeLevels() {
		return treeLevels;
	}

	public void persist() {
		root = BtreeNodeMethods.persist(tran, tran.redir(root), treeLevels);
	}

}
