/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.util.Comparator;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.Range;

/**
 * Compares Range<Record> by lowerEndpoint and then upperEndpoint
 */
@Immutable
public class RangeComparator<T extends Comparable<? super T>>
		implements Comparator<Range<T >> {

	@Override
	public int compare(Range<T> r1, Range<T> r2) {
		int cmp = r1.lowerEndpoint().compareTo(r2.lowerEndpoint());
		return (cmp != 0) ? cmp
				: r1.upperEndpoint().compareTo(r2.upperEndpoint());
	}

}
