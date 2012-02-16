/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import com.google.common.base.Objects;

/** used to return the results of a split */
class BtreeSplit2 {
	final int level; // of node being split
	final BtreeNode left;
	final BtreeNode right;
	final boolean leftSame;
	final Record key; // new value to go in parent, points to right half

	BtreeSplit2(int level, BtreeNode left, BtreeNode right, boolean leftSame, Record key) {
		this.level = level;
		this.left = left;
		this.right = right;
		this.leftSame = leftSame;
		this.key = key;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("level", level)
				.add("left", left)
				.add("right", right)
				.add("leftSame", leftSame)
				.add("key", key)
				.toString();
	}

	static BtreeSplit2 split(BtreeNode node, Record key) {
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
		return new BtreeSplit2(node.level, left, right, leftSame, splitKey);
	}

	private static boolean splitMaxAdr(Record last, Record first) {
		return ! last.prefixEquals(first, first.size() - 1);
	}


}