/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.util.ArrayList;
import java.util.Collections;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;
import com.google.common.collect.Ranges;

/**
 * Accumulates reads done by a transaction.
 * build returns a TransactionReadSet used to validate reads.
 */
public class TransactionReadList {
	static final RangeComparator<Record> cmp = new RangeComparator<Record>();
	static final Ordering<Record> ord = Ordering.natural();
	private final ArrayList<Range<Record>> list  = Lists.newArrayList();

	void add(Range<Record> range) {
		list.add(range);
	}

	TransactionReadSet build() {
		list.trimToSize();
		Collections.sort(list, cmp);
		mergeOverlapping();
		return new TransactionReadSet(list);
	}

	private void mergeOverlapping() {
		int i = 0;
		while (i < list.size()) {
			int first = i;
			++i;
			while (i < list.size() && list.get(i).isConnected(list.get(i - 1)))
				++i;
			int last = i - 1;
			if (last > first) {
				Record lo = list.get(first).lowerEndpoint();
				Record hi = list.get(first).upperEndpoint();
				for (int j = first + 1; j <= last; ++j) {
					lo = ord.min(lo, list.get(j).lowerEndpoint());
					hi = ord.max(hi, list.get(j).upperEndpoint());
				}
				// replace overlapping with duplicates
				// faster than deleting from ArrayList
				Range<Record> merged = Ranges.closed(lo, hi);
				for (int j = first; j <= last; ++j)
					list.set(j, merged);
			}
		}
	}

}
