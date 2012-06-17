/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StorageTest {
	private final Storage stor = new MemStorage(64, 64);

	@Test
	public void test_advance() {
		int adr1 = stor.alloc(40);
		stor.alloc(40); // in second chunk
		int size = (int) stor.sizeFrom(adr1);
		int adr3 = stor.alloc(4);
		stor.buffer(adr3).putInt(12345678);
		int adr3b = stor.advance(adr1, size);
		assertEquals(adr3, adr3b);
		assertEquals(12345678, stor.buffer(adr3b).getInt());
	}

}
