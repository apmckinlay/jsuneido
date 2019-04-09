/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.google.common.base.Strings;

public class ConcatsTest {

	@Test
	public void construct() {
		Concats a = new Concats("hello", "world");
		assertThat(a.toString(), equalTo("helloworld"));
	}

	@Test
	public void append() {
		Concats a = new Concats("hello", "world");
		Concats b = a.append("!");
		assertThat(b.toString(), equalTo("helloworld!"));
	}

	@Test
	public void unshare() {
		Concats a = new Concats("hello", "world");
		Concats b = a.append("!");
		Concats c = a.append("?");
		assertThat(c.toString(), equalTo("helloworld?"));
		assertThat(b.toString(), equalTo("helloworld!"));
	}

	@Test
	public void compact() {
		final int N = 1000;
		Concats a = new Concats("x", "x");
		for (int i = 2; i < N; ++i)
			a = a.append("x");
		String s = a.toString();
		assertThat(s.length(), equalTo(N));
		assertThat(s, equalTo(Strings.repeat("x", N)));
	}

}
