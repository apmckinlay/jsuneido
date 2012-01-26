/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import suneido.util.MergeTree;

import com.google.common.collect.Range;

/**
 * Accumulates reads done by a transaction.
 * build returns a TransactionReadSet used to validate reads.
 */
public class TransactionReadList {
	static final RangeComparator<Record> cmp = new RangeComparator<Record>();
	private final MergeTree<Range<Record>> list  = new MergeTree<Range<Record>>(cmp);

	void add(Range<Record> range) {
		list.add(range);
	}

	TransactionReadSet build() {
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
		return new TransactionReadSet(a, i);
	}

}
