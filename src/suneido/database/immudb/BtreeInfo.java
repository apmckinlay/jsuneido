/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import javax.annotation.concurrent.Immutable;

@Immutable
public class BtreeInfo {
	public final int root;
	public final int treeLevels;
	public final int nnodes;

	public BtreeInfo(int root, int treeLevels, int nnodes) {
		this.root = root;
		this.treeLevels = treeLevels;
		this.nnodes = nnodes;
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
}