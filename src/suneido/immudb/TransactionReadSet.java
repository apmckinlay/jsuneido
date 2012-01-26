/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.util.Arrays;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.Range;

/**
 * Used to validate a transaction's reads.
 * Checks whether values (keys) are contained in any of the ranges.
 * list must be sorted
 * and must not have overlapping ranges other than duplicates
 */
@Immutable
public class TransactionReadSet {
	private final Range<Record>[] reads;
	private final int rlen;

	TransactionReadSet(Range<Record>[] reads, int rlen) {
		this.reads = reads;
		this.rlen = rlen;
	}

	boolean contains(Record rec) {
		int i = lowerBound(rec);
		return i < rlen && reads[i].contains(rec);
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
