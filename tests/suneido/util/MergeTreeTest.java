/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.*;

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

		MergeTree<Integer> mt = new MergeTree<Integer>();
		for (Integer key : keys)
			mt.add(key);
		assertThat(mt.size(), is(NKEYS));
		Collections.sort(keys);

//mt.print();
		int i = 0;
		MergeTree<Integer>.Iter iter = mt.iter();
		for (Integer x = iter.next(); x != null; x = iter.next()) {
			assertThat("i=" + i, x, is(keys.get(i++)));
		}
		assertThat(i, is(NKEYS));

		i = NKEYS;
		iter = mt.iter();
		for (Integer x = iter.prev(); x != null; x = iter.prev()) {
//iter.print();
			assertThat("i=" + i, x, is(keys.get(--i)));
		}
		assertThat(i, is(0));
	}

	@Test
	public void seek() {
		MergeTree<Integer> mt = new MergeTree<Integer>();
		for (int i = 0; i < 10; ++i)
			mt.add(i);
		mt.add(5); // dup

		MergeTree<Integer>.Iter iter = mt.iter();
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
		MergeTree<Integer> mt = new MergeTree<Integer>();
		for (int i = 0; i < 6; ++i)
			mt.add(i);
mt.print();
		MergeTree<Integer>.Iter iter = mt.iter();
		assertThat(iter.next(), is(0));
		assertThat(iter.prev(), is((Integer) null));
		assertThat(iter.next(), is(0));

		iter = mt.iter();
		assertThat(iter.prev(), is(5));
		assertThat(iter.prev(), is(4));
		assertThat(iter.next(), is(5));
		assertNull(iter.next());
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
	public void stable_sort() {
		MergeTree<Ob> mt = new MergeTree<Ob>();
		for (int k = 0; k < 2; ++k)
			for (int d = 0; d < 10; ++d)
				mt.add(new Ob(k, d));
		MergeTree<Ob>.Iter iter = mt.iter();
		for (int k = 0; k < 2; ++k)
			for (int d = 0; d < 10; ++d)
				assertThat(iter.next().toString(), is(k + "," + d));
		iter = mt.iter();
		for (int k = 1; k >= 0; --k)
			for (int d = 9; d >= 0; --d)
				assertThat(iter.prev().toString(), is(k + "," + d));
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
