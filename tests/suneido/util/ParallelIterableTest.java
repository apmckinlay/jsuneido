/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.util.*;

import org.junit.Test;

public class ParallelIterableTest {

	@Test
	@SuppressWarnings("unchecked")
	public void test() {
		List<Integer> list1 = Arrays.asList(1, 2, 3);
		List<Integer> list2 = Arrays.asList(4, 5, 6);
		List<Integer> list3 = Arrays.asList(7, 8);

		Iterator<List<Integer>> iter =
				ParallelIterable.of(list1, list2, list3).iterator();
		assertThat(iter.next(), is(Arrays.asList(1, 4, 7)));
		assertThat(iter.next(), is(Arrays.asList(2, 5, 8)));
		assertThat(iter.next(), is(Arrays.asList(3, 6, null)));
		assertFalse(iter.hasNext());
	}

}
