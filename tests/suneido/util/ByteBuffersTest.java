/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static suneido.util.ByteBuffers.*;

import java.nio.ByteBuffer;

import org.junit.Test;

public class ByteBuffersTest {

	@Test
	public void bufferToString_test() {
		String s = "hello world";
		ByteBuffer buf = ByteBuffer.wrap(s.getBytes());
		assertEquals(s, bufferToString(buf));
	}

	@Test
	public void bufferUcompare_test() {
		byte[][] values = {
				new byte[] {},
				new byte[] { 12 },
				new byte[] { 12, 34 },
				new byte[] { (byte) 0xee },
				new byte[] { (byte) 0xee, 12 } };
		for (int i = 0; i < values.length; ++i) {
			ByteBuf buf1 = ByteBuf.wrap(values[i]);
			assertEquals(0, bufferUcompare(buf1, buf1));
			for (int j = i + 1; j < values.length; ++j) {
				ByteBuf buf2 = ByteBuf.wrap(values[j]);
				assertTrue(i + "," + j, bufferUcompare(buf1, buf2) < 0);
				assertTrue(i + "," + j, bufferUcompare(buf2, buf1) > 0);
			}

		}
	}
	
	@Test
	public void slice_test() {
		byte[] array = { 11, 22, 33, 44 };
		ByteBuffer buf = ByteBuffer.wrap(array);
		ByteBuffer buf2 = slice(buf, 0, 4);
		assertEquals(buf, buf2);
		
		ByteBuffer dbuf = ByteBuffer.allocateDirect(100);
		dbuf.put(array).put(array);
		buf2 = slice(dbuf, 4, 4);
		assertEquals(buf, buf2);
	}

}
