/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import static org.junit.Assert.assertEquals;
import static suneido.util.ByteBuffers.bufferToString;
import static suneido.util.ByteBuffers.slice;

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
