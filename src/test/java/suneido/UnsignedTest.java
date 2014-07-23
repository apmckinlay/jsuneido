/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.primitives.UnsignedInts;

public class UnsignedTest {

	@Test
	public void test() {
		long n = 0xffffffffL;
		int i = (int) n;
		assert i < 0;
		long n2 = UnsignedInts.toLong(i);
		assertEquals(n, n2);
	}

}
