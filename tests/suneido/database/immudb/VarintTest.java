/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

public class VarintTest {
	private final byte[] buf = new byte[5];

	@Test
	public void main() {
		assertEquals(1, Varint.length(0));
		assertEquals(1, Varint.length(1));
		assertEquals(2, Varint.length(200));
		assertEquals(5, Varint.length(0xffffffff));

		test(0);
		test(1);
		test(-1);
		test(Integer.MAX_VALUE);
		test(Integer.MIN_VALUE);
	}

	@Test
	public void random() {
		Random rand = new Random();
		for (int i = 0; i < 10000; ++i)
			test(rand.nextInt());
	}

	private void test(int n) {
		Varint.encode(buf, n);
		assertEquals(n, Varint.decode(buf));
	}

}
