/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import static org.junit.Assert.assertEquals;
import static suneido.util.ByteBuffers.bufferToString;

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
	public void bufferToString_unsigned() {
		ByteBuffer buf = ByteBuffer.allocate(1);
		buf.put((byte) 255);
		buf.flip();
		String s = bufferToString(buf);
		assertEquals(255, s.charAt(0));
	}

	@Test
	public void slice_test() {
		byte[] array = { 11, 22, 33, 44 };
		ByteBuffer buf = ByteBuffer.wrap(array);
		ByteBuffer buf2 = buf.slice(0, 4);
		assertEquals(buf, buf2);

		ByteBuffer dbuf = ByteBuffer.allocateDirect(100);
		dbuf.put(array).put(array);
		buf2 = dbuf.slice(4, 4);
		assertEquals(buf, buf2);
	}

	@Test
	public void varint_test() {
		vi(0);
		vi(1);
		vi(123);
		vi(1234);
		vi(12345678);
		vi(1234567890);
	}

	private static void vi(long n) {
		var buf = ByteBuffer.allocate(10);
		ByteBuffers.putUVarint(buf, n);
		buf.rewind();
		var n2 = ByteBuffers.getUVarint(buf);
		assertEquals(n2, n);
		buf.rewind();
		ByteBuffers.putVarint(buf, n);
		buf.rewind();
		n2 = ByteBuffers.getVarint(buf);
		assertEquals(n2, n);
		buf.rewind();
		ByteBuffers.putVarint(buf, -n);
		buf.rewind();
		n2 = ByteBuffers.getVarint(buf);
		assertEquals(n2, -n);
	}

}
