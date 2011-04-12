/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Random;

import org.junit.Test;


public class DbHashTreeTest {

	@Test
	public void empty() {
		DbHashTree tree = DbHashTree.empty(null);
		for (int i = 32; i < 64; ++i)
			assertThat(tree.get(i), is(0));
	}

	@Test
	public void one_node() {
		DbHashTree tree = DbHashTree.empty(null);
		for (int i = 32; i < 64; ++i)
			tree = tree.with(i, i * 7);
		for (int i = 32; i < 64; ++i)
			assertThat(tree.get(i), is(i * 7));

		// update
		tree = tree.with(50, 555);
		assertThat(tree.get(50), is(555));
	}

	@Test
	public void update_existing() {
		DbHashTree tree = DbHashTree.empty(null);
		tree = tree.with(123, 456);
		tree = tree.with(789, 987);
		assertThat(tree.get(123), is(456));
		assertThat(tree.get(789), is(987));
		tree = tree.with(123, 321); // new value
		tree = tree.with(789, 987); // same value
		assertThat(tree.get(123), is(321));
		assertThat(tree.get(789), is(987));
	}

	@Test
	public void collisions() {
		DbHashTree tree = DbHashTree.empty(null);
		tree = tree.with(0x10000, 123);
		tree = tree.with(0x20000, 456);
		assertThat(tree.get(0x10000), is(123));
		assertThat(tree.get(0x20000), is(456));
	}

	@Test
	public void random() {
		DbHashTree tree = DbHashTree.empty(null);
		Random r = new Random(123);
		int key, value;
		final int N = 10000;
		for (int i = 0; i < N; ++i) {
			key = r.nextInt();
			value = r.nextInt();
			assert tree.get(key) == 0;
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

	@Test
	public void persist() {
		Translator translator = new Translator() {
			public int translate(int x) {
				return x;
			}
		};
		Storage stor = new TestStorage(512, 64);

		DbHashTree tree = DbHashTree.empty(stor);
		int at = tree.store(stor, translator);

		tree = DbHashTree.from(stor, at);
		for (int i = 32; i < 64; ++i)
			assertThat(tree.get(i), is(0));
		tree = tree.with(32, 123).with(64, 456);
		assertThat(tree.get(32), is(123));
		assertThat(tree.get(64), is(456));
		int at2 = tree.store(stor, translator);
		assert(at != at2);
		assertThat(tree.get(32), is(123));
		assertThat(tree.get(64), is(456));
		tree = DbHashTree.from(stor, at2);
		assertThat(tree.get(32), is(123));
		assertThat(tree.get(64), is(456));

		tree = DbHashTree.from(stor, at2);
		assertThat(tree.get(32), is(123));
		assertThat(tree.get(64), is(456));
		Random r = new Random(1234);
		int key, value;
		final int N = 1000;
		for (int i = 0; i < N; ++i) {
			key = r.nextInt();
			value = r.nextInt();
			assert tree.get(key) == 0;
			if (key == 0 || value == 0)
				continue ;
			tree = tree.with(key, value);
		}
		at2 = tree.store(stor, translator);

		tree = DbHashTree.from(stor, at2);
		for (int i = 0; i < N; ++i) {
			key = r.nextInt();
			value = r.nextInt();
			assert tree.get(key) == 0;
			if (key == 0 || value == 0)
				continue ;
			tree = tree.with(key, value);
		}
		r.setSeed(1234);
		for (int i = 0; i < N * 2; ++i) {
			key = r.nextInt();
			value = r.nextInt();
			if (key == 0 || value == 0)
				continue ;
			assertThat("i=" + i + " key=" + DbHashTree.fmt(key) + " value=" + value,
					tree.get(key), is(value));
		}
	}

}
