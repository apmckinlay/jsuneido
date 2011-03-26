/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.Immutable;

/**
 * A list of values stored in a ByteBuffer in the same format as cSuneido.
 * Values are stored using {@link suneido.language.Pack}
 */
@Immutable
public class DbRecord extends Record {
	static class Mode {
		final static byte BYTE = 'c';
		final static byte SHORT = 's';
		final static byte INT = 'l';
	}
	static class Offset {
		final static int TYPE = 0; // byte
		final static int NFIELDS = 2; // short
		final static int SIZE = 4; // byte, short, or int <= type
	}
	private final ByteBuffer buf;
	private final int offset;

	public DbRecord(ByteBuffer buf, int offset) {
		this.buf = buf;
		this.offset = offset;
		assert offset >= 0;
		assert mode() == 'c' || mode() == 's' || mode() == 'l';
		assert size() >= 0;
		assert length() > 0 : "length " + length();
		assert offset < buf.capacity();
		assert offset + length() <= buf.capacity()
			: "offset " + offset + " + length " + length() + " > capacity " + buf.capacity();
	}

	private byte mode() {
		return buf.get(offset + Offset.TYPE);
	}

	@Override
	public int size() {
		int si = offset + Offset.NFIELDS;
		return (buf.get(si) & 0xff) + ((buf.get(si + 1) & 0xff) << 8);
	}

	@Override
	public ByteBuffer fieldBuffer(int i) {
		return buf;
	}

	@Override
	public int fieldLength(int i) {
		if (i >= size())
			return 0;
		return fieldOffset(i - 1) - fieldOffset(i);
	}

	@Override
	public int fieldOffset(int i) {
		// to match cSuneido use little endian (least significant first)
		switch (mode()) {
		case Mode.BYTE:
			return offset + (buf.get(offset + Offset.SIZE + i + 1) & 0xff);
		case Mode.SHORT:
			int si = offset + Offset.SIZE + 2 * (i + 1);
			return offset + ((buf.get(si) & 0xff) + ((buf.get(si + 1) & 0xff) << 8));
		case Mode.INT:
			int ii = offset + Offset.SIZE + 4 * (i + 1);
			return offset + ((buf.get(ii) & 0xff) |
					((buf.get(ii + 1) & 0xff) << 8) |
			 		((buf.get(ii + 2) & 0xff) << 16) |
			 		((buf.get(ii + 3) & 0xff) << 24));
		default:
			throw new Error("invalid record type: " + mode());
		}
	}

	@Override
	public int length() {
		return fieldOffset(-1) - offset;
	}

	@Override
	public int store(Storage stor) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void pack(ByteBuffer dst) {
		for (int i = 0; i < length(); ++i)
			dst.put(buf.get(offset + i));
	}

	public String toDebugString() {
		String s = "";
		s += "type: " + (char) mode() +
				" size: " + size() +
				" length: " + length();
		for (int i = 0; i < Math.min(size(), 10); ++i)
			System.out.println("offset " + i + ": " + fieldOffset(i));
		return s;
	}

}
