/* Copyright 2019 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static suneido.util.testing.Benchmark.benchmark;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import com.google.common.collect.Lists;

public class BlockListTest {
	private Random rand = new Random();
	private BlockList b = new BlockList();

	@Test
	public void test() {
		assertThat(b.size(), equalTo(0));
		b.sort(); // empty
		int n = addRandom(1);
		assertThat(b.size(), equalTo(1));
		n += addRandom(BlockList.BLOCKSIZE / 2);
		assertThat(b.size(), equalTo(n));
		b.sort(); // single block
		for (int i = 1; i < n; ++i)
			assert b.get(i-1) <= b.get(i);
		n += addRandom(BlockList.BLOCKSIZE * 11);
		b.sort(); // multiple blocks
		assert b.size() == n;
		int prev = b.get(0);
		for (int i = 1; i < n; ++i) {
			var x = b.get(i);
			assert x >= prev;
			prev = x;
		}

		var iter = b.iter();
		for (int i = 0; i < n; ++i)
			assertThat(iter.next(), equalTo(b.get(i)));

		iter = b.iter();
		for (int i = n-1; i >= 0; --i)
			assertThat(iter.prev(), equalTo(b.get(i)));
	}

	@Test
	public void indirect() {
		String[] vals = new String[]{"joe", "amy", "bob"};
		var b = new BlockList((i,j) -> vals[i].compareTo(vals[j]));
		b.add(0);
		b.add(1);
		b.add(2);
		b.sort();
		assertThat(b.get(0), equalTo(1)); // amy
		assertThat(b.get(1), equalTo(2)); // bob
		assertThat(b.get(2), equalTo(0)); // joe
	}

	@Test
	public void seek() {
		BlockList b = new BlockList();
		for (int i = 0; i < 10; ++i)
			b.add(i);
		b.add(5); // dup
		b.sort();

		BlockList.Iter iter = b.iter();
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
		BlockList b = new BlockList();
		for (int i = 0; i < 6; ++i)
			b.add(i);
		b.sort();

		BlockList.Iter iter = b.iter();
		assertThat(iter.next(), equalTo(0));
		assertThat(iter.prev(), equalTo(Integer.MIN_VALUE));
		assertThat(iter.next(), equalTo(0));

		iter = b.iter();
		assertThat(iter.prev(), equalTo(5));
		assertThat(iter.prev(), equalTo(4));
		assertThat(iter.next(), equalTo(5));
		assertThat(iter.next(), equalTo(Integer.MAX_VALUE));
		assertThat(iter.prev(), equalTo(5));

		iter = b.iter();
		assertThat(iter.prev(), equalTo(5));
		assertThat(iter.prev(), equalTo(4));
		assertThat(iter.prev(), equalTo(3));
		assertThat(iter.next(), equalTo(4));

		iter = b.iter();
		assertThat(iter.next(), equalTo(0));
		assertThat(iter.next(), equalTo(1));
		assertThat(iter.next(), equalTo(2));
		assertThat(iter.next(), equalTo(3));
		assertThat(iter.next(), equalTo(4));
		assertThat(iter.prev(), equalTo(3));
	}

	@Test
	public void switch_direction2() {
		BlockList b = new BlockList();
		final int N = 1023;
		for (int i = 0; i < N; ++i)
			b.add(i);
		b.sort();
		BlockList.Iter iter = b.iter();
		for (int i = 0; i < N - 1; ++i) {
			assertThat(iter.next(), equalTo(i));
			assertThat(iter.next(), equalTo(i + 1));
			assertThat(iter.prev(), equalTo(i));
		}
		iter = b.iter();
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
			BlockList b = new BlockList();
			for (int i = 0; i < NKEYS; ++i)
				b.add(rand.nextInt(NKEYS));
			b.sort();
			List<Integer> keys = Lists.newArrayList();
			BlockList.Iter iter = b.iter();
			for (int i = 0; i < NKEYS / 5; ++i)
				keys.add(iter.prev());
			iter.prev();
			Collections.reverse(keys);
			for (int i = 0; i < NKEYS / 5; ++i) {
				assertThat(iter.next(), equalTo(keys.get(i)));
			}
			assertThat(iter.next(), equalTo(Integer.MAX_VALUE));
		}
	}

	private int addRandom(int n) {
		for (int i = 0; i < n; ++i)
			b.add(rand.nextInt());
		return n;
	}

	@Test
	public void benchmarkBlockList() {
		benchmark("BlockList", (long nreps) -> {
			final int N = 111_111;
			while (nreps-- > 0) {
				b = new BlockList();
				addRandom(N);
				b.sort();

				var iter = b.iter();
				int n = 0;
				while (Integer.MAX_VALUE != iter.next())
					++n;
				assert n == N;

				for (int i = 0; i < N; ++i) {
					iter.seekFirst(rand.nextInt());
					iter.next();
				}
			}
		});
	}

}
