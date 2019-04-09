/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static suneido.database.immudb.Storage.MAX_ADR;
import static suneido.database.immudb.Storage.adrToOffset;
import static suneido.database.immudb.Storage.offsetToAdr;

import org.junit.Test;

public class StorageTest {
	private final Storage stor = new HeapStorage(64);

	@Test
	public void test_advance() {
		int adr1 = stor.alloc(40);
		stor.alloc(40); // in second chunk
		long size = stor.sizeFrom(adr1);
		int adr3 = stor.alloc(4);
		stor.buffer(adr3).putInt(12345678);
		int adr3b = stor.advance(adr1, size);
		assertEquals(adr3, adr3b);
		assertEquals(12345678, stor.buffer(adr3b).getInt());
	}

	@Test
	public void test_grow() {
		for (int i = 0; i < stor.INIT_CHUNKS * 3; ++i)
			stor.buffer(stor.alloc(40)).putInt(12345678);
	}

	@Test
	public void test_convert() {
		long gb = 1024L * 1024 * 1024;
		long gb10 = 10L * gb;
		long gb20 = 20L * gb;

		assertThat(adrToOffset(offsetToAdr(gb10)), equalTo(gb10));
		assertThat(adrToOffset(offsetToAdr(gb20)), equalTo(gb20));

		assert Integer.compareUnsigned(offsetToAdr(0), offsetToAdr(gb10)) < 0;
		assert Integer.compareUnsigned(offsetToAdr(0), offsetToAdr(gb20)) < 0;
		assert Integer.compareUnsigned(offsetToAdr(0), MAX_ADR) < 0;
		assert Integer.compareUnsigned(offsetToAdr(gb10), offsetToAdr(gb20)) < 0;
		assert Integer.compareUnsigned(offsetToAdr(gb10), MAX_ADR) < 0;
		assert Integer.compareUnsigned(offsetToAdr(gb20), MAX_ADR) < 0;

		assert adrToOffset(offsetToAdr(gb10) - 1) < gb10;
		assert adrToOffset(offsetToAdr(gb20) - 1) < gb20;
	}

}
