/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.immudb.IntRefs;

public class IntRefsTest {

	@Test
	public void random() {
		IntRefs ir = new IntRefs();
		final int N = 1000;
		Object[] refs = new Object[N];
		int[] ints = new int[N];
		for (int i = 0; i < N; ++i) {
			refs[i] = new Object();
			ints[i] = ir.refToInt(refs[i]);
		}
		for (int i = 0; i < N; ++i) {
			assertEquals(refs[i], ir.intToRef(ints[i]));
		}
	}

}
