/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.ByteBuffer;

import org.junit.Test;

import suneido.util.FileUtils;

public class MmapFileTest {

	@Test(expected = RuntimeException.class)
	public void cant_open() {
		new MmapFile("must_not_exist", "r");
	}

	@Test
	public void main() {
		File tmp = FileUtils.tempfile();
		MmapFile mmf = new MmapFile(tmp, "rw");
		final int N = 128;
		int adr = mmf.alloc(N);
		ByteBuffer buf = mmf.buffer(adr);
		for (int i = 0; i < N; ++i)
			buf.put(i, (byte) i);
		mmf.close();

		mmf = new MmapFile(tmp, "r");
		buf = mmf.rbuffer(-N);
		for (int i = 0; i < N; ++i)
			assertEquals(i, buf.get(i));
		mmf.close();
	}

}
