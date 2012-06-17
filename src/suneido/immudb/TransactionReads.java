/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.util.Arrays;

import suneido.util.MergeTree;

/**
 * Accumulates reads done by a transaction on a single index.
 * Operates in two stages, first simply accumulating a list of reads.
 * Then build combines overlapping ranges.
 * After build, contains can be used to validate a transaction's reads.
 */
class TransactionReads {
	private final MergeTree<IndexRange> list  = new MergeTree<IndexRange>();
	private IndexRange[] reads;
	private int rlen;

	void add(IndexRange keyRange) {
		list.add(keyRange);
	}

	boolean isEmpty() {
		return list.size() == 0;
	}

	void build() {
		int i = 0;
		IndexRange[] a = new IndexRange[list.size()];
		if (a.length > 0) {
			MergeTree<IndexRange>.Iter iter = list.iter();
			IndexRange prev = iter.next();
			IndexRange x;
			while (null != (x = iter.next()))
				if (prev.isConnected(x))
					prev.extendToSpan(x);
				else {
					a[i++] = prev;
					prev = x;
				}
			a[i++] = prev;
		}
		list.clear();
		reads = a;
		rlen = i;
	}

	/**
	 * Uses a binary search.
	 * Note: Cannot be used until after build
	 * @param key is assumed to have the correct number of fields for the index
	 * @return Whether or not the key is contained in any of the ranges.
	 */
	boolean contains(Record key) {
		int i = lowerBound(key);
		return i < rlen && reads[i].contains(key);
	}

	// use our own binary search so we can compare Range to Record
	private int lowerBound(Record value) {
		int first = 0;
		int len = rlen;
		while (len > 0) {
			int half = len >> 1;
			int middle = first + half;
			if (compare(reads[middle], value) < 0) {
				first = middle + 1;
				len -= half + 1;
			} else
				len = half;
		}
		return first;
	}

	private static int compare(IndexRange range, Record value) {
		if (range.lo.compareTo(value) > 0)
			return +1;
		return range.hi.compareTo(value) < 0 ? -1 : +1;
	}

	@Override
	public String toString() {
		return Arrays.toString(Arrays.copyOf(reads, rlen));
	}

}
