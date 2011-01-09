/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.io.IOException;
import java.io.Writer;


/**
 * @see BtreeLeafNode, BtreeTreeNode
 */
public class Btree {
	private static final int MAX_NODE_SIZE = 20;
	private static final int MAX_LEVELS = 20;
	private int root = 0; // an IntRef
	private int treeLevels = 0;

	/**
	 * @param key A key without the final record address.
	 * @return The record address or 0 if the key wasn't found.
	 */
	public int get(Record key) {
		int adr = root;
		BtreeTreeNode treeNodes[] = new BtreeTreeNode[MAX_LEVELS];
		int i;
		for (i = 0; i < treeLevels; ++i) {
			treeNodes[i] = treeNodeAt(adr);
			Record slot = treeNodes[i].find(key);
			adr = getAddress(slot);
		}
		BtreeLeafNode leaf = leafNodeAt(adr);
		Record slot = leaf.find(key);
		return slot.startsWith(key) ? getAddress(slot) : 0;
	}

	public void add(Record key) {
		if (root == 0) {
			BtreeLeafNode leaf = BtreeLeafNode.EMPTY.with(key);
			root = Tran.refToInt(leaf);
			return;
		}

		int adr = root;
		// search down the tree
		BtreeTreeNode treeNodes[] = new BtreeTreeNode[MAX_LEVELS];
		int adrs[] = new int[MAX_LEVELS];
		int i;
		for (i = 0; i < treeLevels; ++i) {
			adrs[i] = adr;
			treeNodes[i] = treeNodeAt(adr);
			Record slot = treeNodes[i].find(key);
			adr = getAddress(slot);
		}

		BtreeLeafNode leaf = leafNodeAt(adr);
		if (leaf.size() < MAX_NODE_SIZE) {
			leaf = leaf.with(key);
			Tran.redir(adr, leaf);
			return;

		}
		// else split leaf
		Split split = leaf.split(key, adr);

		// insert up the tree
		for (--i; i >= 0; --i) {
			BtreeTreeNode tree = treeNodes[i];
			if (tree.size() < MAX_NODE_SIZE) {
				tree = tree.with(split.key);
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
		BtreeTreeNode node = BtreeTreeNode.newRoot(split);
		return Tran.refToInt(node);
	}

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

	static BtreeTreeNode treeNodeAt(int adr) {
		adr = Tran.redir(adr);
		return IntRefs.isIntRef(adr)
			? (BtreeTreeNode) Tran.intToRef(adr)
			: new BtreeTreeNode(Tran.mmf().buffer(adr));
	}

	static BtreeLeafNode leafNodeAt(int adr) {
		adr = Tran.redir(adr);
		return IntRefs.isIntRef(adr)
			? (BtreeLeafNode) Tran.intToRef(adr)
			: new BtreeLeafNode(Tran.mmf().buffer(adr));
	}

	public void print(Writer writer) {
		try {
			BtreeNode node = treeLevels == 0 ? leafNodeAt(root) : treeNodeAt(root);
			node.print(writer, treeLevels);
			writer.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
