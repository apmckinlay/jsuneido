/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.Random;

import org.junit.Test;

import suneido.database.immudb.DbHashTrie.Entry;
import suneido.database.immudb.DbHashTrie.IntEntry;
import suneido.database.immudb.DbHashTrie.Translator;

public class DbHashTrieTest {
	DbHashTrie tree = DbHashTrie.empty(null);

	DbHashTrie.Entry entry(int key, int value) {
		return new IntEntry(key, value);
	}

	@Test
	public void empty() {
		assertFalse(tree.stored());
		assertTrue(tree.immutable());
		for (int i = 32; i < 64; ++i)
			assertNull(tree.get(i));
	}

	@Test
	public void one_entry() {
		add(123, 456);
		assertThat(get(123), equalTo(456));
		assertFalse(tree.immutable());
	}

	@Test
	public void one_node() {
		for (int i = 32; i < 64; ++i)
			add(i, i * 7);
		for (int i = 32; i < 64; ++i)
			assertThat(tree.get(i), equalTo(entry(i, i * 7)));

		// update
		add(50, 555);
		assertThat(tree.get(50), equalTo(entry(50, 555)));
	}

	@Test
	public void update_existing() {
		add(123, 456);
		add(789, 987);
		assertThat(tree.get(123), equalTo(entry(123, 456)));
		assertThat(tree.get(789), equalTo(entry(789, 987)));
		add(123, 321); // new value
		add(789, 987); // same value
		assertThat(tree.get(123), equalTo(entry(123, 321)));
		assertThat(tree.get(789), equalTo(entry(789, 987)));
	}

	@Test
	public void collisions() {
		add(0x10000, 123);
		add(0x20000, 456);
		assertThat(get(0x10000), equalTo(123));
		assertThat(get(0x20000), equalTo(456));
	}

	void add(int key, int value) {
		tree = tree.with(entry(key, value));
	}

	int get(int key) {
		IntEntry e = (IntEntry) tree.get(key);
		return (e == null) ? 0 : e.value;
	}

	@Test
	public void random() {
		Random rand = new Random(123);
		int key, value;
		final int N = 10000;
		addRandom(rand, N);
		rand.setSeed(123);
		for (int i = 0; i < N; ++i) {
			key = rand.nextInt();
			value = rand.nextInt();
			if (key == 0 || value == 0)
				continue ;
			assertThat(get(key), equalTo(value));
		}
	}

	private void addRandom(Random rand, final int N) {
		Random r = new Random(789);
		for (int i = 0; i < N; ++i) {
			int key = rand.nextInt();
			int value = rand.nextInt();
			assert tree.get(key) == null; // no dups
			if (key == 0 || value == 0)
				continue ;
			add(key, value);
			if (r.nextInt(91) == 71)
				tree.freeze();
		}
	}

	Translator nullTranslator = (Entry e) -> e;

	@Test
	public void store() {
		Storage stor = new HeapStorage();
		tree = DbHashTrie.empty(stor);

		int at = tree.store(nullTranslator);

		tree = DbHashTrie.load(stor, at, nullTranslator);
		for (int i = 32; i < 64; ++i)
			assertNull(tree.get(i));
		add(32, 123);
		add(64, 456);
		assertThat(get(32), equalTo(123));
		assertThat(get(64), equalTo(456));
		int at2 = tree.store(nullTranslator);

		assert(at != at2);
		assertThat(get(32), equalTo(123));
		assertThat(get(64), equalTo(456));

		tree = DbHashTrie.load(stor, at2, nullTranslator);
		assertThat(get(32), equalTo(123));
		assertThat(get(64), equalTo(456));

		tree = DbHashTrie.load(stor, at2, nullTranslator);
		assertThat(get(32), equalTo(123));
		assertThat(get(64), equalTo(456));
		Random rand = new Random(1234);
		int key, value;
		final int N = 1000;
		addRandom(rand, N);
		at2 = tree.store(nullTranslator);

		tree = DbHashTrie.load(stor, at2, nullTranslator);
		addRandom(rand, N);
		rand.setSeed(1234);
		for (int i = 0; i < N * 2; ++i) {
			key = rand.nextInt();
			value = rand.nextInt();
			if (key == 0 || value == 0)
				continue ;
			assertThat(get(key), equalTo(value));
		}
	}

	@Test
	public void traverse() {
		Storage stor = new HeapStorage();
		tree = DbHashTrie.empty(stor);
		DbHashTrie.Process proc = mock(DbHashTrie.Process.class);

		tree.traverseUnstored(proc);
		verifyZeroInteractions(proc);

		add(123, 456);
		tree.traverseUnstored(proc);
		verify(proc).apply(entry(123, 456));

		Random rand = new Random(46578);
		addRandom(rand, 100);
		int adr = tree.store(nullTranslator);
		tree = DbHashTrie.load(stor, adr, nullTranslator);
		proc = mock(DbHashTrie.Process.class);
		tree.traverseUnstored(proc);
		verifyZeroInteractions(proc);

		final int N = 10;
		Entry[] entries = new Entry[N];
		for (int i = 0; i < N; ++i) {
			entries[i] = entry(rand.nextInt(), rand.nextInt());
			tree = tree.with(entries[i]);
		}
		proc = mock(DbHashTrie.Process.class);
		tree.traverseUnstored(proc);
		for (int i = 0; i < N; ++i)
			verify(proc).apply(entries[i]);
	}

}
