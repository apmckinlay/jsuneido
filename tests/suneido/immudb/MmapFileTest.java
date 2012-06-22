/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.ByteBuffer;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class MmapFileTest {

	@Test(expected = RuntimeException.class)
	public void cant_open() {
		new MmapFile("tmp1", "r");
	}

	@Test
	public void main() {
		MmapFile mmf = new MmapFile("tmp2", "rw");
		final int N = 128;
		int adr = mmf.alloc(N);
		ByteBuffer buf = mmf.buffer(adr);
		for (int i = 0; i < N; ++i)
			buf.put(i, (byte) i);
		mmf.close();

		mmf = new MmapFile("tmp2", "r");
		buf = mmf.rbuffer(-N);
		for (int i = 0; i < N; ++i)
			assertEquals(i, buf.get(i));
		mmf.close();
	}

	@BeforeClass
	@AfterClass
	public static void cleanup() {
		for (int i = 1; i <= 2; ++i)
			new File("tmp" + i).delete();
	}

}
