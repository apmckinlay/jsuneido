/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.google.common.base.Strings;

// NOTE: remember toString flattens

public class ConcatsTest {

	@Test
	public void construct() {
		Concats a = new Concats("hello", "world");
		assertThat(a.toString(), is("helloworld"));
	}

	@Test
	public void append() {
		Concats a = new Concats("hello", "world");
		Concats b = a.append("!");
		assertThat(b.toString(), is("helloworld!"));
	}

	@Test
	public void unshare() {
		Concats a = new Concats("hello", "world");
		Concats b = a.append("!");
		Concats c = a.append("?");
		assertThat(c.toString(), is("helloworld?"));
		assertThat(b.toString(), is("helloworld!"));
	}

	@Test
	public void compact() {
		final int N = 1000;
		Concats a = new Concats("x", "x");
		for (int i = 2; i < N; ++i)
			a = a.append("x");
		String s = a.toString();
		assertThat(s.length(), is(N));
		assertThat(s, is(Strings.repeat("x", N)));
	}

}
