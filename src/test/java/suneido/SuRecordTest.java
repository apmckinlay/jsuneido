/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.runtime.Pack;


public class SuRecordTest {

	@Test
	public void test_pack() {
		SuRecord x = new SuRecord();
		assertEquals(x, Pack.unpack(Pack.pack(x)));
		x.put("a", 123);
		assertEquals(x, Pack.unpack(Pack.pack(x)));
	}

}
