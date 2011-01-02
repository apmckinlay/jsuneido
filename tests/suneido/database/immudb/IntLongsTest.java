/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.junit.Assert.assertEquals;
import static suneido.database.immudb.IntLongs.*;

import java.util.Random;

import org.junit.Test;

public class IntLongsTest {

	@Test
	public void main() {
		assertEquals(0, intToLong(0));
		assertEquals(0, longToInt(0));
		test(0);
		test(16);
		test(32);
		test(MAX);
	}

	private void test(long n) {
		int i = longToInt(n);
		assertEquals(n, intToLong(i));
	}

	@Test
	public void random() {
		Random rand = new Random();
		for (int i = 0; i < 10000; ++i) {
			int n = rand.nextInt();
			assertEquals(n, longToInt(intToLong(n)));
		}
	}

	@Test(expected = AssertionError.class)
	public void too_big() {
		longToInt(MAX + 1);
	}

	@Test(expected = AssertionError.class)
	public void negative() {
		longToInt(-1);
	}

	@Test(expected = AssertionError.class)
	public void not_aligned() {
		longToInt(0x1234567);
	}

}
