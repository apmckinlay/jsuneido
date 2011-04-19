/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import java.util.Random;

import org.junit.Test;

public class DbHashTreeTest {
	Context context = new Context(null);

	@Test
	public void empty() {
		DbHashTree tree = DbHashTree.empty(context);
		for (int i = 32; i < 64; ++i)
			assertThat(tree.get(i), is(0));
	}

	@Test
	public void one_node() {
		DbHashTree tree = DbHashTree.empty(context);
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
		DbHashTree tree = DbHashTree.empty(context);
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
		DbHashTree tree = DbHashTree.empty(context);
		tree = tree.with(0x10000, 123);
		tree = tree.with(0x20000, 456);
		assertThat(tree.get(0x10000), is(123));
		assertThat(tree.get(0x20000), is(456));
	}

	@Test
	public void random() {
		DbHashTree tree = DbHashTree.empty(context);
		Random rand = new Random(123);
		int key, value;
		final int N = 10000;
		tree = addRandom(tree, rand, N);
		rand.setSeed(123);
		for (int i = 0; i < N; ++i) {
			key = rand.nextInt();
			value = rand.nextInt();
			if (key == 0 || value == 0)
				continue ;
			assertThat(tree.get(key), is(value));
		}
	}

	private DbHashTree addRandom(DbHashTree tree, Random rand, final int N) {
		for (int i = 0; i < N; ++i) {
			int key = rand.nextInt();
			int value = rand.nextInt();
			assert tree.get(key) == 0;
			if (key == 0 || value == 0)
				continue ;
			tree = tree.with(key, value);
		}
		return tree;
	}

	@Test
	public void store() {
		Translator translator = new NullTranslator();
		Context context = new Context(new TestStorage(512, 64));

		DbHashTree tree = DbHashTree.empty(context);
		int at = tree.store(translator);

		tree = DbHashTree.from(context, at);
		for (int i = 32; i < 64; ++i)
			assertThat(tree.get(i), is(0));
		tree = tree.with(32, 123).with(64, 456);
		assertThat(tree.get(32), is(123));
		assertThat(tree.get(64), is(456));
		int at2 = tree.store(translator);
		assert(at != at2);
		assertThat(tree.get(32), is(123));
		assertThat(tree.get(64), is(456));
		tree = DbHashTree.from(context, at2);
		assertThat(tree.get(32), is(123));
		assertThat(tree.get(64), is(456));

		tree = DbHashTree.from(context, at2);
		assertThat(tree.get(32), is(123));
		assertThat(tree.get(64), is(456));
		Random r = new Random(1234);
		int key, value;
		final int N = 1000;
		tree = addRandom(tree, r, N);
		at2 = tree.store(translator);

		tree = DbHashTree.from(context, at2);
		tree = addRandom(tree, r, N);
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

	private class NullTranslator implements Translator {
		public int translate(int x) {
			return x;
		}
	}

	@Test
	public void traverse() {
		Context context = new Context(new TestStorage(512, 64));
		DbHashTree tree = DbHashTree.empty(context);
		DbHashTree.Process proc = mock(DbHashTree.Process.class);

		tree.traverseChanges(proc);
		verifyZeroInteractions(proc);

		int val = context.intrefs.refToInt(null);
		tree = tree.with(1, val);
		tree.traverseChanges(proc);
		verify(proc).apply(1, val);

		Random rand = new Random(46578);
		tree = addRandom(tree, rand, 100);
		Translator translator = new NullTranslator();
		int adr = tree.store(translator);
		tree = DbHashTree.from(context, adr);
		proc = mock(DbHashTree.Process.class);
		tree.traverseChanges(proc);
		verifyZeroInteractions(proc);

		final int N = 10;
		int adrs[] = new int[N];
		int vals[] = new int[N];
		for (int i = 0; i < N; ++i) {
			adrs[i] = rand.nextInt();
			vals[i] = context.intrefs.refToInt(null);
			tree = tree.with(adrs[i], vals[i]);
		}
		proc = mock(DbHashTree.Process.class);
		tree.traverseChanges(proc);
		for (int i = 0; i < N; ++i)
			verify(proc).apply(adrs[i], vals[i]);
	}

}
