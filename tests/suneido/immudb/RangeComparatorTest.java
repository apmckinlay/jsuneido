/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.google.common.collect.Range;
import com.google.common.collect.Ranges;

public class RangeComparatorTest {
	private static RangeComparator<Integer> cmp = new RangeComparator<Integer>();

	@Test
	public void test() {
		assertThat(cmp(range(123, 456), range(234, 400)), lessThan(0));
		assertThat(cmp(range(234, 400), range(123, 456)), greaterThan(0));
		assertThat(cmp(range(123, 456), range(123, 789)), lessThan(0));
		assertThat(cmp(range(123, 789), range(123, 456)), greaterThan(0));
	}

	private static int cmp(Range<Integer> r1, Range<Integer> r2) {
		return cmp.compare(r1, r2);
	}

	private static Range<Integer>range(int lo, int hi) {
		return Ranges.closed(lo, hi);
	}

}
