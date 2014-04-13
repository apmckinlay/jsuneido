/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

import org.junit.Test;

public class IndexRangeTest {

	@Test
	public void single() {
		assert range(123, 123).contains(rec(123));
		assert new IndexRange(DatabasePackage.MIN_RECORD, DatabasePackage.MAX_RECORD)
			.contains(rec(1));
	}

	@Test
	public void compare() {
		assertThat(range(123, 123).compareTo(range(123, 123)), equalTo(0));
		assertThat(range(123, 789).compareTo(range(456, 789)), lessThan(0));
		assertThat(range(456, 789).compareTo(range(123, 789)), greaterThan(0));
		assertThat(range(123, 456).compareTo(range(123, 789)), lessThan(0));
		assertThat(range(123, 789).compareTo(range(123, 456)), greaterThan(0));
	}

	IndexRange range(int lo, int hi) {
		return new IndexRange(rec(lo), rec(hi));
	}

	Record rec(int n) {
		return new RecordBuilder().add(n).build();
	}

}
