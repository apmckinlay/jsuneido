/* Copyright 2016 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import static suneido.util.ByteBuffers.getStringFromBuffer;

import java.nio.ByteBuffer;
import java.util.List;

import com.google.common.collect.ImmutableList;

import suneido.runtime.Pack;
import suneido.util.ByteBuffers;

/**
 * Handles serialization for the *binary* client-server protocol.
 * Used by {@link SuChannel}.
 * put methods return this so they can be chained.
 */
public abstract class Serializer {
	abstract ByteBuffer allow(int n);
	abstract void putBuffer(ByteBuffer buf);
	abstract ByteBuffer need(int n);
	abstract void forceNewBuffer(int n);

	// writing -----------------------------------------------------------------

	public Serializer put(boolean b) {
		ByteBuffer buf = allow(1);
		buf.put((byte) (b ? 1 : 0));
		return this;
	}

	public Serializer putByte(byte b) {
		ByteBuffer buf = allow(1);
		buf.put(b);
		return this;
	}

	/** varint encoding */
	public Serializer put(long n) {
		ByteBuffer buf = allow(10); // 64 bits / 7 bits per byte
		n = (n << 1) ^ (n >> 63); // zig zag encoding
		while ((n & ~0x7FL) != 0) {
			buf.put((byte) ((n & 0x7F) | 0x80));
			n >>>= 7;
		}
		buf.put((byte) (n & 0x7F));
		return this;
	}

	/** Put a size prefixed string (8 bit chars) */
	public Serializer put(String s) {
		ByteBuffer buf = putAllow(s.length());
		ByteBuffers.putStringToByteBuffer(s, buf);
		return this;
	}

	/** Put a size prefixed array of bytes */
	public Serializer put(byte[] b) {
		ByteBuffer buf = putAllow(b.length);
		buf.put(b);
		return this;
	}

	/**
	 * Put a size prefixed ByteBuffer.
	 * NOTE: Reference to buffer may be held until write.
	 * The application must not modify it.
	 * See also {@link SuChannel#putBuffer}
	 */
	public Serializer put(ByteBuffer src) {
		int n = src.remaining();
		put(n);
		putBuffer(src);
		return this;
	}

	public Serializer putPacked(Object value) {
		ByteBuffer buf = putAllow(Pack.packSize(value));
		Pack.pack(value, buf);
		return this;
	}

	/** Put a size prefixed sequence of put(int) */
	public Serializer putInts(List<Integer> list) {
		put(list.size());
		for (Integer n : list)
			put(n.intValue());
		return this;
	}

	/** Put a size prefixed sequence of put(String) */
	public Serializer putStrings(List<String> list) {
		put(list.size());
		for (String s : list)
			put(s);
		return this;
	}

	protected ByteBuffer putAllow(int n) {
		put(n);
		return allow(n);
	}

	// reading -----------------------------------------------------------------

	public boolean getBool() {
		switch (get1()) {
		case 0: return false;
		case 1: return true;
		default: throw new RuntimeException("bad boolean value");
		}
	}

	public byte getByte() {
		return get1();
	}

	public int getInt() {
		return (int) getLong();
	}

	/** varint decoding */
	public long getLong() {
		long n = 0L;
		int i = 0;
		byte b;
		while (((b = get1()) & 0x80L) != 0) {
			n |= (long) (b & 0x7F) << i;
			i += 7;
			assert i <= 63;
		}
		n |= ((long) b << i);
		long tmp = (((n << 63) >> 63) ^ n) >> 1;
		return tmp ^ (n & (1L << 63));
	}

	/** Get a size prefixed string (8 byte chars) */
	public String getString() {
		int n = getInt();
		ByteBuffer buf = need(n);
		return getStringFromBuffer(buf, n);
	}

	/** Get a size prefixed array of bytes */
	public byte[] getBytes() {
		int n = getInt();
		ByteBuffer buf = need(n);
		byte[] b = new byte[n];
		buf.get(b);
		return b;
	}

	/**
	 * @return A ByteBuffer that is owned by the caller
	 * and will stay valid after the request.
	 * Preferable to getBuffer + copy for large chunks
	 * because it reads directly into the buffer.
	 */
	public ByteBuffer getOwnedBuffer() {
		int n = getInt();
		forceNewBuffer(n);
		return getBuffer(n);
	}

	/**
	 * Get a size prefixed buffer.
	 * @return A new ByteBuffer
	 * but referencing a portion of the io buffer
	 * so it will be invalidated after the request.
	 */
	public ByteBuffer getBuffer() {
		int n = getInt();
		return getBuffer(n);
	}

	/**
	 * Get a buffer with a known size.
	 * @return A new ByteBuffer
	 * but referencing a portion of the io buffer
	 * so it will be invalidated after the request.
	 */
	public ByteBuffer getBuffer(int n) {
		ByteBuffer buf = need(n);
		ByteBuffer b = buf.duplicate();
		buf.position(buf.position() + n);
		b.limit(buf.position());
		return b;
	}

	public Object getPacked() {
		return Pack.unpack(getBuffer());
	}

	/** Get a size prefixed sequence of getInt */
	public List<Integer> getInts() {
		int n = getInt();
		ImmutableList.Builder<Integer> builder = ImmutableList.builder();
		for (int i = 0; i < n; ++i)
			builder.add(getInt());
		return builder.build();
	}

	/** Get a size prefixed sequence of getString */
	public List<String> getStrings() {
		int n = getInt();
		ImmutableList.Builder<String> builder = ImmutableList.builder();
		for (int i = 0; i < n; ++i)
			builder.add(getString());
		return builder.build();
	}

	/** Get a single byte, reading if necessary */
	private byte get1() {
		ByteBuffer buf = need(1);
		return buf.get();
	}

}
