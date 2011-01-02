/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.*;

import suneido.util.ByteBuf;

public class MmapFileTest {

	@Test(expected = RuntimeException.class)
	public void cant_open() {
		new MmapFile("tmp1", "r");
	}

	@Test
	public void main() {
		MmapFile mmf = new MmapFile("tmp2", "rw");
		assertEquals(0, mmf.size());
		long offset = mmf.alloc(100);
		ByteBuf buf = mmf.buf(offset);
		for (byte i = 0; i < 100; ++i)
			buf.put(i, i);
		mmf.close();

		mmf = new MmapFile("tmp2", "r");
		assertEquals(100, mmf.size());
		buf = mmf.buf(offset);
		for (byte i = 0; i < 100; ++i)
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
