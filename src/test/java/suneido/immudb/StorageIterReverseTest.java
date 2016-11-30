/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

import org.junit.Test;

public class StorageIterReverseTest {
	Storage stor = new HeapStorage(64);

	@Test
	public void test() {
		add(40);
		// there will be padding between these
		add(32);
		add(24);
		StorageIterReverse iter = new StorageIterReverse(stor);
		int n = 0;
		for (; iter.hasPrev(); ++n)
			iter.prev();
		assertEquals(3, n);
	}

	private void add(final int N) {
		ByteBuffer buf = stor.buffer(stor.alloc(N));
		buf.putInt(0, Storage.sizeToInt(N)); // leading size
		buf.putInt(N - Integer.BYTES, Storage.sizeToInt(N)); // trailing size
	}

}
