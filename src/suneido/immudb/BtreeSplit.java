/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import com.google.common.base.Objects;

/** used to return the results of a split */
class BtreeSplit {
	final int level; // of node being split
	final int left;
	final Record key; // new value to go in parent, points to right half

	BtreeSplit(int level, int left, Record key) {
		this.level = level;
		this.left = left;
		this.key = key;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("level", level)
				.add("left", left)
				.add("oldkey", key)
				.toString();
	}
}