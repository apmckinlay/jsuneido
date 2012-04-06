/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import com.google.common.collect.Ordering;

/**
 * A mutable range of records.
 * Used to track the range of an index scanned by an iterator.
 * Needs to be mutable because it is passed to the iterator which updates it.
 * Based somewhat on Guava Range (but it's immutable)
 */
class IndexRange implements Comparable<IndexRange> {
	private static final Ordering<Record> ord = Ordering.natural();
	Record lo;
	Record hi;

	/** create an empty range (lo > hi) */
	public IndexRange() {
		lo = DatabasePackage.MAX_RECORD;
		hi = DatabasePackage.MIN_RECORD;
	}

	public IndexRange(Record lo, Record hi) {
		this.lo = lo;
		this.hi = hi;
	}

	boolean isConnected(IndexRange that) {
		return this.contains(that.lo) || that.contains(this.lo);
	}

	public void extendToSpan(IndexRange that) {
		lo = ord.min(this.lo, that.lo);
		hi = ord.max(this.hi, that.hi);
	}

	public boolean contains(Record key) {
		return lo.compareTo(key) <= 0 && hi.compareTo(key) >= 0;
	}

	@Override
	public int compareTo(IndexRange that) {
		int cmp = this.lo.compareTo(that.lo);
		return (cmp != 0) ? cmp : this.hi.compareTo(that.hi);
	}

	@Override
	public String toString() {
		return "[" + lo + ".." + hi + "]";
	}

}
