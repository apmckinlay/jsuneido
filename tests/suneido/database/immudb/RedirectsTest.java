/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Random;

import org.junit.After;
import org.junit.Test;

public class RedirectsTest {

	@Test
	public void main() {
		Redirects r = new Redirects();
		assertThat(r.get(123), equalTo(123));
		r.put(123, 456);
		assertThat(r.get(123), equalTo(456));

		Random rand = new Random(8907);
		final int N = 100;
		int from[] = new int[N];
		int to[] = new int[N];
		for (int i = 0; i < N; ++i) {
			from[i] = rand.nextInt() | 1; // ensure non-zero
			to[i] = rand.nextInt() | 1; // ensure non-zero
			r.put(from[i], to[i]);
		}
		for (int i = N - 1; i >= 0; --i)
			assertThat(r.get(from[i]), equalTo(to[i]));
	}

	@After
	public void teardown() {
		Tran.remove();
	}

}
