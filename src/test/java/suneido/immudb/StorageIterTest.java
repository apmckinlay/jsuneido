/* Copyright 2015 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

import org.junit.Test;

public class StorageIterTest {
	Storage stor = new HeapStorage(64);

	@Test
	public void test_bug_20150901() {
		add(56);
		add(32); // header will go in first chunk
		add(24);
		add(16);
		StorageIter iter = new StorageIter(stor);
		int n = 0;
		for (; ! iter.finished(); iter.advance2())
			++n;
		assertEquals(StorageIter.Status.OK, iter.status());
		assertEquals(4, n);
	}

	private void add(final int N) {
		int adr = stor.alloc(8); // header
		int start = adr;
		ByteBuffer buf = stor.buffer(adr);
		buf.putInt(0, N); // leading size
		buf.putInt(4, 1234); // dummy timestamp (0 is aborted and skipped)

		if (N > 16)
			stor.alloc(N - 16); // body

		adr = stor.alloc(8); // trailer
		buf = stor.buffer(adr);
		buf.putInt(0, stor.checksum(start));
		buf.putInt(4, N); // trailing size
	}

}
