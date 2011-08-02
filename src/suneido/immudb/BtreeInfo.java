/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static com.google.common.base.Preconditions.checkArgument;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Objects;

/**
 * root, treeLevels, and nnodes
 */
@Immutable
class BtreeInfo {
	final int root;
	final int treeLevels;
	final int nnodes;
	final int totalSize;

	BtreeInfo(int root, int treeLevels, int nnodes, int totalSize) {
		checkArgument(root != 0);
		checkArgument(treeLevels >= 0);
		checkArgument(nnodes > 0);
		this.root = root;
		this.treeLevels = treeLevels;
		this.nnodes = nnodes;
		this.totalSize = totalSize;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof BtreeInfo) {
			BtreeInfo that = (BtreeInfo) other;
			return this.root == that.root &&
					this.treeLevels == that.treeLevels &&
					this.nnodes == that.nnodes;
		}
		return false;
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("root", root)
			.add("treeLevels", treeLevels)
			.add("nnodes", nnodes)
			.toString();
	}

}