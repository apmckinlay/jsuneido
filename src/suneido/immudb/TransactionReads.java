/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.util.Arrays;

import suneido.util.MergeTree;

import com.google.common.collect.Range;

/**
 * Accumulates reads done by a transaction on a single index.
 * Operates in two stages, first simply accumulating a list of reads.
 * Then build combines overlapping ranges.
 * After build, contains can be used to validate a transaction's reads.
 */
public class TransactionReads {
	static final RangeComparator<Record> cmp = new RangeComparator<Record>();
	private final MergeTree<Range<Record>> list  = new MergeTree<Range<Record>>(cmp);
	private Range<Record>[] reads;
	private int rlen;

	void add(Range<Record> keyRange) {
		list.add(keyRange);
	}

	boolean isEmpty() {
		return list.size() == 0;
	}

	void build() {
		int i = 0;
		@SuppressWarnings("unchecked")
		Range<Record>[] a = new Range[list.size()];
		if (a.length > 0) {
			MergeTree<Range<Record>>.Iter iter = list.iter();
			Range<Record> prev = iter.next();
			Range<Record> x;
			while (null != (x = iter.next()))
				if (prev.isConnected(x))
					prev = prev.span(x);
				else {
					a[i++] = prev;
					prev = x;
				}
			a[i++] = prev;
		}
		reads = a;
		rlen = i;
	}

	/**
	 * Uses a binary search.
	 * Note: Cannot be used until after build
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

	private static int compare(Range<Record> range, Record value) {
		int cmp = range.lowerEndpoint().compareTo(value);
		return cmp != 0 ? cmp : -1;
	}

	@Override
	public String toString() {
		return Arrays.toString(Arrays.copyOf(reads, rlen));
	}

}
