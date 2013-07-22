/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Random;

import org.junit.Test;

public class PersistentIntMapTest {

	@Test
	public void empty() {
		PersistentIntMap<Integer> tree = PersistentIntMap.empty();
		for (int i = 32; i < 64; ++i)
			assertThat(tree.get(i), is((Integer) null));
	}

	@Test
	public void one_node() {
		PersistentIntMap<Integer> tree = PersistentIntMap.empty();
		for (int i = 32; i < 64; ++i)
			tree = tree.with(i, i * 7);
		for (int i = 32; i < 64; ++i)
			assertThat(tree.get(i), is((i * 7)));

		// update
		tree = tree.with(50, 555);
		assertThat(tree.get(50), is(555));
	}

	@Test
	public void collisions() {
		PersistentIntMap<Integer> tree = PersistentIntMap.empty();
		tree = tree.with(0x10000, 123);
		tree = tree.with(0x20000, 456);
		assertThat(tree.get(0x10000), is(123));
		assertThat(tree.get(0x20000), is(456));
	}

	@Test
	public void random() {
		PersistentIntMap<Integer> tree = PersistentIntMap.empty();
		Random r = new Random(123);
		int key, value;
		final int N = 10000;
		for (int i = 0; i < N; ++i) {
			key = r.nextInt();
			value = r.nextInt();
			assert tree.get(key) == null;
			if (key == 0 || value == 0)
				continue ;
			tree = tree.with(key, value);
		}
		r.setSeed(123);
		for (int i = 0; i < N; ++i) {
			key = r.nextInt();
			value = r.nextInt();
			if (key == 0 || value == 0)
				continue ;
			assertThat(tree.get(key), is(value));
		}
	}

}
