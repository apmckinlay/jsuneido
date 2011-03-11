/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.io.*;

/**
 * An append-only immutable btree.
 * <p>
 * Note: If a key is unique without it's data address
 * then it can be updated via redirection
 * otherwise it must be updated by delete and insert
 * since it's position may change, potentially to a different node.
 * @see BtreeNode, BtreeLeafNode, BtreeTreeNode, BtreeNodeMethods
 */
public class Btree {
	public static final int MAX_NODE_SIZE = 20;
	private static final int MAX_LEVELS = 20;
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
			adr = getAddress(slot);
		}
		BtreeNode leaf = nodeAt(0, adr);
		Record slot = leaf.find(key);
		return slot != null && slot.startsWith(key) ? getAddress(slot) : 0;
	}

	public void add(DbRecord key) {
		// search down the tree
		int adr = root;
		BtreeNode treeNodes[] = new BtreeNode[MAX_LEVELS];
		int adrs[] = new int[MAX_LEVELS];
		int i;
		for (i = 0; i < treeLevels; ++i) {
			adrs[i] = adr;
			int level = treeLevels - i;
			treeNodes[i] = nodeAt(level, adr);
			Record slot = treeNodes[i].find(key);
			adr = getAddress(slot);
		}

		BtreeNode leaf = nodeAt(0, adr);
		if (leaf.size() < MAX_NODE_SIZE) {
			// normal/fast path - simply insert into leaf
			BtreeNode before = leaf;
			leaf = leaf.with(key);
			if (adr == root) {
				if (leaf != before)
					root = tran.refToInt(leaf);
			} else
				tran.redir(adr, leaf);
//if (adr == root) {
//root = tran.redir(root);
//rootNode = leaf;
//}
			return;
		}
		// else split leaf
		Split split = leaf.split(tran, key, adr);

		// insert up the tree
		for (--i; i >= 0; --i) {
			BtreeNode tree = treeNodes[i];
			if (tree.size() < MAX_NODE_SIZE) {
				tree = tree.with(split.key);
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
		newRoot(split);
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
//		BtreeNode[] nodes = Iterables.toArray(
//				Iterables.filter(tran.intrefs, BtreeNode.class),
//				BtreeNode.class);
//		Arrays.sort(nodes, nodeLevelComparator);
//		for (BtreeNode node : nodes)
//			node.store(tran);

//System.out.println("STORE");
		// very crude and inefficient
		for (int level = 0; level <= treeLevels; ++level) {
//System.out.println("level " + level);
			int i = -1;
			for (Object x : tran.intrefs) {
				++i;
				if (x instanceof BtreeNode) {
					BtreeNode node = (BtreeNode) x;
					if (node.level() == level) {
						int adr = node.store(tran);
//System.out.println((i | IntRefs.MASK) + " stored at " + adr);
						if (adr != 0)
							tran.setAdr(i | IntRefs.MASK, adr);
					}
				}
			}
		}
//System.out.println("root before " + root);
		if (IntRefs.isIntRef(root))
			root = tran.getAdr(root);
//System.out.println("root after " + root);
	}

//	private static final Comparator<BtreeNode> nodeLevelComparator =
//		new Comparator<BtreeNode>() {
//			@Override
//			public int compare(BtreeNode x, BtreeNode y) {
//				return y.level() - x.level(); // reverse, biggest first
//			}
//		};

}
