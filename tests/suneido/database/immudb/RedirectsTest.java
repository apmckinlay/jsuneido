/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

import java.util.Random;

import org.junit.Test;

public class RedirectsTest {

	@Test
	public void main() {
		DbHashTree tree = mock(DbHashTree.class);
		when(tree.get(123)).thenReturn(456);
		when(tree.with(anyInt(), anyInt())).thenReturn(tree);
		Redirects r = new Redirects(tree);
		assertThat(r.get(123), is(456));
		r.put(123, 456);
		verify(tree).with(123, 456);
	}

	@Test
	public void merge_with_no_concurrent_change() {
		DbHashTree original = mock(DbHashTree.class);
		Redirects r = new Redirects(original);
		r.put(123, 456);
		DbHashTree redirs = r.redirs();
		r.merge(original);
		assertThat(r.redirs(), is(redirs));
	}

	@Test(expected = Redirects.Conflict.class)
	public void merge_conflict() {
		IntRefs intrefs = new IntRefs();
		DbHashTree master = DbHashTree.empty(null);
		Redirects r = new Redirects(master);
		r.put(123, intrefs.refToInt(null));
		master = master.with(123, intrefs.refToInt(null));
		r.merge(master);
	}

	@Test
	public void merge() {
		IntRefs intrefs = new IntRefs();
		DbHashTree master = DbHashTree.empty(new Context(null));
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
			vals[i] = intrefs.refToInt(null);
			master = master.with(adrs[i], vals[i]);
		}
		r.merge(master);
		for (int i = 0; i < N; ++i)
			assertThat(r.get(adrs[i]), is(vals[i]));
	}

}
