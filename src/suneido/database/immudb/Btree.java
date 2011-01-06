/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;



/**
 * @see BtreeLeafNode, BtreeTreeNode
 */
public class Btree {
	private static final int MAX_LEAF_SIZE = 20;
	private static final int MAX_LEVELS = 20;
	private int root = 0; // an IntRef
	private int treeLevels = 0;

	public void add(Record key) {
System.out.println("add " + key);
		if (root == 0) {
			BtreeLeafNode leaf = BtreeLeafNode.EMPTY.with(key);
			root = IntRefs.refToInt(leaf);
			return;
		}

		int adr = root;
		int[] intRefs = new int[MAX_LEVELS];
		BtreeTreeNode treeNodes[] = new BtreeTreeNode[MAX_LEVELS];
		int i;
		for (i = 0; i < treeLevels; ++i) {
			intRefs[i] = adr;
			treeNodes[i] = (BtreeTreeNode) IntRefs.intToRef(adr);
			Record slot = treeNodes[i].find(key);
			adr = getAddress(slot);
		}

		BtreeLeafNode leaf = (BtreeLeafNode) IntRefs.intToRef(adr);
		if (leaf.size() < MAX_LEAF_SIZE) {
			leaf = leaf.with(key);
System.out.println("leaf " + leaf);
			if (adr == root)
				root = IntRefs.refToInt(leaf);
		} else { // split
			Split split = leaf.split();

			for (--i; i >= 0; --i) {
assert false : "not done yet";
			}

			root = newRoot(split);
		}
	}

	private int getAddress(Record slot) {
		return (Integer) slot.get(slot.size() - 1);
	}

	private int newRoot(Split split) {
		++treeLevels;
		BtreeTreeNode node = BtreeTreeNode.newRoot(split);
System.out.println("newRoot " + node);
assert split.left == getAddress(node.get(0));
assert split.right == getAddress(node.get(1));
		return IntRefs.refToInt(node);
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

}
