/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ByteBuffers {

	// buffer to string --------------------------------------------------------

	/**
	 * @return The remainder of the buffer as a string.
	 * Does NOT change buffer position.
	 */
	public static String bufferToString(ByteBuffer buf) {
		return bufferToString(buf, buf.position());
	}

	/**
	 * @return The remainder of the buffer after pos as a string.
	 * Does NOT change buffer position.
	 */
	public static String bufferToString(ByteBuffer buf, int pos) {
		return bufferToString(buf, pos, buf.limit() - pos);
	}

	/**
	 * @return A portion of the buffer as a string.
	 * Does NOT change buffer position.
	 */
	public static String bufferToString(ByteBuffer buf, int pos, int len) {
		if (pos >= buf.limit())
			return "";
		if (buf.hasArray())
			return new String(buf.array(), buf.arrayOffset() + pos, len,
					StandardCharsets.ISO_8859_1);
		else {
			byte[] a = new byte[len];
			buf.get(pos, a);
			return new String(a, 0, len, StandardCharsets.ISO_8859_1);
		}
	}

	/** DOES change buffer position */
	public static String getStringFromBuffer(ByteBuffer buf, int len) {
		if (len > buf.remaining())
			throw new BufferUnderflowException();
		if (buf.hasArray()) {
			String s = new String(buf.array(), buf.arrayOffset() + buf.position(), len,
					StandardCharsets.ISO_8859_1);
			buf.position(buf.position() + len);
			return s;
		} else {
			byte[] a = new byte[len];
			buf.get(a);
			return new String(a, 0, len, StandardCharsets.ISO_8859_1);
		}
	}

	// string to buffer --------------------------------------------------------

	/** @return A buffer with position 0 and limit of string length */
	public static ByteBuffer stringToBuffer(String s) {
		ByteBuffer buf = ByteBuffer.allocate(s.length());
		putStringToByteBuffer(s, buf, 0);
		return buf;
	}

	/** DOES change buffer position */
	public static void putStringToByteBuffer(String s, ByteBuffer buf) {
		for (int i = 0; i < s.length(); ++i)
			buf.put((byte) s.charAt(i));
	}

	/** does NOT change buffer position */
	public static void putStringToByteBuffer(String s, ByteBuffer buf, int pos) {
		for (int i = 0; i < s.length(); ++i)
			buf.put(pos++, (byte) s.charAt(i));
	}

	//--------------------------------------------------------------------------

	public static String bufferToHex(ByteBuffer buf) {
		StringBuilder sb = new StringBuilder();
		for (int i = buf.position(); i < buf.limit(); ++i)
			sb.append(" ").append(String.format("%02x", buf.get(i)));
		return sb.substring(1);
	}

	public static String bufferToHex(byte[] buf, int i, int n) {
		StringBuilder sb = new StringBuilder();
		for (; i < n; ++i)
			sb.append(" ").append(ByteBuffers.format(buf[i]));
		return sb.substring(1);
	}

	private static String format(byte b) {
		StringBuilder s = new StringBuilder(String.format("%02x", b));
		if (32 <= b && b <= 126)
			s.append(" '").append((char) b).append("' ");
		return s.toString();
	}

	/** Unsigned byte compare */
	public static int bufferUcompare(ByteBuffer b1, ByteBuffer b2) {
		int b1pos = b1.position();
		int b2pos = b2.position();
		int b1lim = b1pos + Math.min(b1.remaining(), b2.remaining());
		for (; b1pos < b1lim; ++b1pos, ++b2pos) {
			int cmp = (b1.get(b1pos) & 0xff) - (b2.get(b2pos) & 0xff);
			if (cmp != 0)
				return cmp;
		}
		return b1.remaining() - b2.remaining();
	}

	public static ByteBuffer copyByteBuffer(ByteBuffer buf, int size) {
		byte[] data = new byte[size];
		buf.get(0, data);
		return ByteBuffer.wrap(data);
	}

	public static final ByteBuffer EMPTY_BUF = ByteBuffer.allocate(0);

	public static int indexOf(ByteBuffer buf, byte b) {
		return indexOf(buf, 0, b);
	}

	/**
	 * @return the index of the first b in buf between pos and buf.position()
	 * or -1 if not found.
	 */
	public static int indexOf(ByteBuffer buf, int pos, byte b) {
		for (int i = pos; i < buf.position(); ++i)
			if (buf.get(i) == b)
				return i;
		return -1;
	}

	/**
	 * @return true if remaining() is zero in all the buffers.
	 * Useful when writing buffers to a channel
	 */
	public static boolean bufsEmpty(ByteBuffer[] bufs) {
		for (ByteBuffer b : bufs)
			if (b.remaining() > 0)
				return false;
		return true;
	}

	// varint -------------------------------------------------------

	/** put a varint zig zag encoded long to a ByteBuffer */
	public static void putVarint(ByteBuffer buf, long n) {
		n = (n << 1) ^ (n >> 63); // zig zag encoding
		putUVarint(buf, n);
	}

	/** put an unsigned varint encoded long to a ByteBuffer */
	public static void putUVarint(ByteBuffer buf, long n) {
		while ((n & ~0x7FL) != 0) {
			buf.put((byte) ((n & 0x7F) | 0x80));
			n >>>= 7;
		}
		buf.put((byte) (n & 0x7F));
	}

	/** get a varint zig zag encoded long from a ByteBuffer */
	public static long getVarint(ByteBuffer buf) {
		long n = getUVarint(buf);
		long tmp = (((n << 63) >> 63) ^ n) >> 1;
		return tmp ^ (n & (1L << 63));
	}

	/** get an unsigned varint encoded long from a ByteBuffer */
	public static long getUVarint(ByteBuffer buf) {
		long n = 0L;
		int i = 0;
		byte b;
		while (((b = buf.get()) & 0x80L) != 0) {
			n |= (long) (b & 0x7F) << i;
			i += 7;
			assert i <= 63;
		}
		n |= ((long) b << i);
		return n;
	}

	public static int varintSize(long n) {
		if (n == 0)
			return 1;
		var bits = 64 - Long.numberOfLeadingZeros(n);
		return (bits + 6) / 7;
	}

}
