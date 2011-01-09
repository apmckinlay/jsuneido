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
	private static final int MAX_NODE_SIZE = 20;
	private static final int MAX_LEVELS = 20;
	private int root = 0; // an IntRef
	private int treeLevels = 0;

	public Btree() {
		root = Tran.refToInt(BtreeNode.EMPTY_LEAF);
		treeLevels = 0;
	}

	public Btree(int root, int treeLevels) {
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
			leaf = leaf.with(key);
			if (adr == root)
				root = Tran.refToInt(leaf);
			else
				Tran.redir(adr, leaf);
			return;
		}
		// else split leaf
		Split split = leaf.split(key, adr);

		// insert up the tree
		for (--i; i >= 0; --i) {
			BtreeNode tree = treeNodes[i];
			if (tree.size() < MAX_NODE_SIZE) {
				tree = tree.with(split.key);
				if (adrs[i] == root)
					root = Tran.refToInt(tree);
				else
					Tran.redir(adrs[i], tree);
				return;
			}
			// else split
			split = tree.split(split.key, adrs[i]);
		}
		// getting here means root was split so a new root is needed
		root = newRoot(split);
	}

	private int getAddress(Record slot) {
		return (Integer) slot.get(slot.size() - 1);
	}

	private int newRoot(Split split) {
		++treeLevels;
		BtreeNode node = BtreeNode.newRoot(split);
		return Tran.refToInt(node);
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

	public int persist() {
		if (IntRefs.isIntRef(root)) {
			root = rootNode().persist();
		}
		return root;
	}

	static BtreeNode treeNodeAt(int adr) {
		adr = Tran.redir(adr);
		return IntRefs.isIntRef(adr)
			? (BtreeNode) Tran.intToRef(adr)
			: BtreeNode.tree(Tran.mmf().buffer(adr));
	}

	static BtreeNode leafNodeAt(int adr) {
		adr = Tran.redir(adr);
		return IntRefs.isIntRef(adr)
			? (BtreeNode) Tran.intToRef(adr)
			: BtreeNode.leaf(Tran.mmf().buffer(adr));
	}

	public void print() {
		print(new PrintWriter(System.out));
	}

	public void print(Writer writer) {
		try {
			rootNode().print(writer, treeLevels);
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

}
