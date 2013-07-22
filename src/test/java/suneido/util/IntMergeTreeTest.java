/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import com.google.common.collect.Lists;

public class IntMergeTreeTest {

	@Test
	public void test() {
		Random rand = new Random(98707);

		final int NKEYS = 10;
		int[] keys = new int[NKEYS];
		for (int i = 0; i < NKEYS; ++i)
			keys[i] = rand.nextInt(NKEYS);

		IntMergeTree mt = new IntMergeTree();
		for (int key : keys)
			mt.add(key);
		assertThat(mt.size(), is(NKEYS));
		Arrays.sort(keys);

		int i = 0;
		IntMergeTree.Iter iter = mt.iter();
		for (int x = iter.next(); x != Integer.MAX_VALUE; x = iter.next()) {
			assertThat("i=" + i, x, is(keys[i++]));
		}
		assertThat(i, is(NKEYS));

		i = NKEYS;
		iter = mt.iter();
		for (int x = iter.prev(); x != Integer.MIN_VALUE; x = iter.prev()) {
			assertThat("i=" + i, x, is(keys[--i]));
		}
		assertThat(i, is(0));
	}

	@Test
	public void seek() {
		IntMergeTree mt = new IntMergeTree();
		for (int i = 0; i < 10; ++i)
			mt.add(i);
		mt.add(5); // dup

		IntMergeTree.Iter iter = mt.iter();
		iter.seekFirst(5);
		assertThat(iter.next(), is(5));
		assertThat(iter.next(), is(5));
		assertThat(iter.next(), is(6));

		iter.seekLast(5);
		assertThat(iter.prev(), is(5));
		assertThat(iter.prev(), is(5));
		assertThat(iter.prev(), is(4));
	}

	@Test
	public void switch_direction() {
		IntMergeTree mt = new IntMergeTree();
		for (int i = 0; i < 6; ++i)
			mt.add(i);

		IntMergeTree.Iter iter = mt.iter();
		assertThat(iter.next(), is(0));
		assertThat(iter.prev(), is(Integer.MIN_VALUE));
		assertThat(iter.next(), is(0));

		iter = mt.iter();
		assertThat(iter.prev(), is(5));
		assertThat(iter.prev(), is(4));
		assertThat(iter.next(), is(5));
		assertThat(iter.next(), is(Integer.MAX_VALUE));
		assertThat(iter.prev(), is(5));

		/* tree is:
		 * 		[4, 5]
		 * 		[0, 1, 2, 3]
		 * test reading next/prev at node boundary
		 */
		iter = mt.iter();
		assertThat(iter.prev(), is(5));
		assertThat(iter.prev(), is(4));
		assertThat(iter.prev(), is(3));
		assertThat(iter.next(), is(4));

		iter = mt.iter();
		assertThat(iter.next(), is(0));
		assertThat(iter.next(), is(1));
		assertThat(iter.next(), is(2));
		assertThat(iter.next(), is(3));
		assertThat(iter.next(), is(4));
		assertThat(iter.prev(), is(3));
	}

	@Test
	public void switch_direction2() {
		IntMergeTree mt = new IntMergeTree();
		final int N = 1023;
		for (int i = 0; i < N; ++i)
			mt.add(i);
		IntMergeTree.Iter iter = mt.iter();
		for (int i = 0; i < N - 1; ++i) {
			assertThat(iter.next(), is(i));
			assertThat(iter.next(), is(i + 1));
			assertThat(iter.prev(), is(i));
		}
		iter = mt.iter();
		for (int i = N - 1; i > 0; --i) {
			assertThat(iter.prev(), is(i));
			assertThat(iter.prev(), is(i - 1));
			assertThat(iter.next(), is(i));
		}
	}

	@Test
	public void switch_direction3() {
		Random rand = new Random(98707);
		for (int reps = 0; reps < 100; ++reps) {
			final int NKEYS = 5 + rand.nextInt(95);
			IntMergeTree mt = new IntMergeTree();
			for (int i = 0; i < NKEYS; ++i)
				mt.add(rand.nextInt(NKEYS));
			List<Integer> keys = Lists.newArrayList();
			IntMergeTree.Iter iter = mt.iter();
			for (int i = 0; i < NKEYS / 5; ++i)
				keys.add(iter.prev());
			iter.prev();
			Collections.reverse(keys);
			for (int i = 0; i < NKEYS / 5; ++i) {
				assertThat(iter.next(), is(keys.get(i)));
			}
			assertThat(iter.next(), is(Integer.MAX_VALUE));
		}
	}

}



