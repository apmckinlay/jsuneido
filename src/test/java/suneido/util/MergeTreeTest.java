/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;

public class MergeTreeTest {

	@Test
	public void test() {
		Random rand = new Random(98707);

		ArrayList<Integer> keys = Lists.newArrayList();
		final int NKEYS = 10;
		for (int i = 0; i < NKEYS; ++i)
			keys.add(rand.nextInt(NKEYS));

		MergeTree<Integer> mt = new MergeTree<>();
		for (Integer key : keys)
			mt.add(key);
		assertThat(mt.size(), equalTo(NKEYS));
		Collections.sort(keys);

		int i = 0;
		MergeTree<Integer>.Iter iter = mt.iter();
		for (Integer x = iter.next(); x != null; x = iter.next()) {
			assertThat("i=" + i, x, equalTo(keys.get(i++)));
		}
		assertThat(i, equalTo(NKEYS));

		i = NKEYS;
		iter = mt.iter();
		for (Integer x = iter.prev(); x != null; x = iter.prev()) {
			assertThat("i=" + i, x, equalTo(keys.get(--i)));
		}
		assertThat(i, equalTo(0));
	}

	@Test
	public void seek() {
		MergeTree<Integer> mt = new MergeTree<>();
		for (int i = 0; i < 10; ++i)
			mt.add(i);
		mt.add(5); // dup

		MergeTree<Integer>.Iter iter = mt.iter();
		iter.seekFirst(5);
		assertThat(iter.next(), equalTo(5));
		assertThat(iter.next(), equalTo(5));
		assertThat(iter.next(), equalTo(6));

		iter.seekLast(5);
		assertThat(iter.prev(), equalTo(5));
		assertThat(iter.prev(), equalTo(5));
		assertThat(iter.prev(), equalTo(4));
	}

	@Test
	public void switch_direction() {
		MergeTree<Integer> mt = new MergeTree<>();
		for (int i = 0; i < 6; ++i)
			mt.add(i);

		MergeTree<Integer>.Iter iter = mt.iter();
		assertThat(iter.next(), equalTo(0));
		assertThat(iter.prev(), equalTo((Integer) null));
		assertThat(iter.next(), equalTo(0));

		iter = mt.iter();
		assertThat(iter.prev(), equalTo(5));
		assertThat(iter.prev(), equalTo(4));
		assertThat(iter.next(), equalTo(5));
		assertNull(iter.next());
		assertThat(iter.prev(), equalTo(5));

		/* tree is:
		 * 		[4, 5]
		 * 		[0, 1, 2, 3]
		 * test reading next/prev at node boundary
		 */
		iter = mt.iter();
		assertThat(iter.prev(), equalTo(5));
		assertThat(iter.prev(), equalTo(4));
		assertThat(iter.prev(), equalTo(3));
		assertThat(iter.next(), equalTo(4));

		iter = mt.iter();
		assertThat(iter.next(), equalTo(0));
		assertThat(iter.next(), equalTo(1));
		assertThat(iter.next(), equalTo(2));
		assertThat(iter.next(), equalTo(3));
		assertThat(iter.next(), equalTo(4));
		assertThat(iter.prev(), equalTo(3));
	}

	@Test
	public void switch_direction2() {
		MergeTree<Integer> mt = new MergeTree<>();
		final int N = 1023;
		for (int i = 0; i < N; ++i)
			mt.add(i);
		MergeTree<Integer>.Iter iter = mt.iter();
		for (int i = 0; i < N - 1; ++i) {
			assertThat(iter.next(), equalTo(i));
			assertThat(iter.next(), equalTo(i + 1));
			assertThat(iter.prev(), equalTo(i));
		}
		iter = mt.iter();
		for (int i = N - 1; i > 0; --i) {
			assertThat(iter.prev(), equalTo(i));
			assertThat(iter.prev(), equalTo(i - 1));
			assertThat(iter.next(), equalTo(i));
		}
	}

	@Test
	public void switch_direction3() {
		Random rand = new Random(98707);
		for (int reps = 0; reps < 100; ++reps) {
			final int NKEYS = 5 + rand.nextInt(95);
			MergeTree<Integer> mt = new MergeTree<>();
			for (int i = 0; i < NKEYS; ++i)
				mt.add(rand.nextInt(NKEYS));
			List<Integer> keys = Lists.newArrayList();
			MergeTree<Integer>.Iter iter = mt.iter();
			for (int i = 0; i < NKEYS / 5; ++i)
				keys.add(iter.prev());
			iter.prev();
			Collections.reverse(keys);
			for (int i = 0; i < NKEYS / 5; ++i) {
				assertThat(iter.next(), equalTo(keys.get(i)));
			}
			assertThat(iter.next(), equalTo((Integer) null));
		}
	}

	@Test
	public void stable_sort() {
		MergeTree<Ob> mt = new MergeTree<>();
		for (int k = 0; k < 2; ++k)
			for (int d = 0; d < 10; ++d)
				mt.add(new Ob(k, d));
		MergeTree<Ob>.Iter iter = mt.iter();
		for (int k = 0; k < 2; ++k)
			for (int d = 0; d < 10; ++d)
				assertThat(iter.next().toString(), equalTo(k + "," + d));
		iter = mt.iter();
		for (int k = 1; k >= 0; --k)
			for (int d = 9; d >= 0; --d)
				assertThat(iter.prev().toString(), equalTo(k + "," + d));
	}

	private static class Ob implements Comparable<Ob> {
		final int key;
		final int data;

		Ob(int key, int data) {
			this.key = key;
			this.data = data;
		}

		@Override
		public int compareTo(Ob that) {
			return Ints.compare(this.key, that.key);
		}

		@Override
		public String toString() {
			return key + "," + data;
		}
	}

}
