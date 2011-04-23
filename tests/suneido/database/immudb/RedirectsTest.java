/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.Random;

import org.junit.Test;

import suneido.database.immudb.DbHashTrie.Entry;
import suneido.database.immudb.DbHashTrie.IntEntry;

public class RedirectsTest {

	@Test
	public void main() {
		DbHashTrie tree = mock(DbHashTrie.class);
		when(tree.get(123)).thenReturn(new IntEntry(123, 456));
		when(tree.with(any(Entry.class))).thenReturn(tree);
		Redirects r = new Redirects(tree);
		assertThat(r.get(123), is(456));
		r.put(123, 456);
		verify(tree).with(new IntEntry(123, 456));
	}

	@Test
	public void merge_with_no_concurrent_change() {
		DbHashTrie original = mock(DbHashTrie.class);
		Redirects r = new Redirects(original);
		r.put(123, 456);
		DbHashTrie redirs = r.redirs();
		r.merge(original);
		assertThat(r.redirs(), is(redirs));
	}

	@Test(expected = Redirects.Conflict.class)
	public void merge_conflict() {
		DbHashTrie master = DbHashTrie.empty();
		Redirects r = new Redirects(DbHashTrie.empty());
		r.put(123, 456);
		master = master.with(new IntEntry(123, 789));
		r.merge(master);
	}

	@Test
	public void merge() {
		IntRefs intrefs = new IntRefs();
		DbHashTrie master = DbHashTrie.empty();
		Redirects r = new Redirects(master);
		final int N = 100;
		int adrs[] = new int[N];
		int vals[] = new int[N];
		Random rand = new Random(46578);
		for (int i = 0; i < N / 2; ++i) {
			adrs[i] = rand.nextInt();
			vals[i] = intrefs.refToInt(null);
			r.put(adrs[i], vals[i]);
		}
		for (int i = N / 2; i < N; ++i) {
			adrs[i] = rand.nextInt();
			vals[i] = rand.nextInt();
			master = master.with(new IntEntry(adrs[i], vals[i]));
		}
		r.merge(master);
		for (int i = 0; i < N; ++i)
			assertThat(r.get(adrs[i]), is(vals[i]));
	}

}
