/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static com.google.common.base.Preconditions.checkArgument;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.MoreObjects;

/**
 * Holds root, treeLevels, nnodes, totalsize
 */
@Immutable
class BtreeInfo {
	final int root;
	final int treeLevels;
	final int nnodes;
	final int totalSize;
	final BtreeNode rootNode;

	BtreeInfo(int root, BtreeNode rootNode, int treeLevels, int nnodes, int totalSize) {
		checkArgument(root != 0 || rootNode != null);
		checkArgument(treeLevels >= 0);
		checkArgument(nnodes > 0);
		this.root = root;
		this.treeLevels = treeLevels;
		this.nnodes = nnodes;
		this.totalSize = totalSize;
		this.rootNode = rootNode;
	}

	BtreeInfo(int root, int treeLevels, int nnodes, int totalSize) {
		this(root, null, treeLevels, nnodes, totalSize);
	}

	@Override
	public boolean equals(Object other) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("root", root)
			.add("treeLevels", treeLevels)
			.add("nnodes", nnodes)
			.add("totalSize", totalSize)
			.toString();
	}

}