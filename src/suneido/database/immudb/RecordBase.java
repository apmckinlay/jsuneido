/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.Immutable;

@Immutable
public class RecordBase implements Bufferable {
	protected static final ByteBuffer emptyRecBuf = new RecordBuilder().asByteBuffer();
	public final ByteBuffer buf;
	public final int offset;

	protected RecordBase() {
		this(emptyRecBuf, 0);
	}

	public RecordBase(ByteBuffer buf, int offset) {
		this.buf = buf;
		this.offset = offset;
	}

	byte mode() {
		return mode(buf, offset);
	}
	static byte mode(ByteBuffer buf, int offset) {
		return buf.get(offset + Offset.TYPE);
	}

	public int fieldLength(int i) {
		return fieldLength(buf, offset, i);
	}
	public static int fieldLength(ByteBuffer buf, int offset, int i) {
		if (i >= size(buf, offset))
			return 0;
		return fieldOffset(buf, offset, i - 1) - fieldOffset(buf, offset, i);
	}

	public int size() {
		return size(buf, offset);
	}
	public static int size(ByteBuffer buf, int offset) {
		int si = offset + Offset.NFIELDS;
		return (buf.get(si) & 0xff) + ((buf.get(si + 1) & 0xff) << 8);
	}


	public int fieldOffset(int i) {
		return fieldOffset(buf, offset, i);
	}
	protected static int fieldOffset(ByteBuffer buf, int offset, int i) {
		// to match cSuneido use little endian (least significant first)
		switch (mode(buf, offset)) {
		case Mode.BYTE:
			return buf.get(offset + Offset.SIZE + i + 1) & 0xff;
		case Mode.SHORT:
			int si = offset + Offset.SIZE + 2 * (i + 1);
			return (buf.get(si) & 0xff) + ((buf.get(si + 1) & 0xff) << 8);
		case Mode.INT:
			int ii = offset + Offset.SIZE + 4 * (i + 1);
			return (buf.get(ii) & 0xff) |
					((buf.get(ii + 1) & 0xff) << 8) |
			 		((buf.get(ii + 2) & 0xff) << 16) |
			 		((buf.get(ii + 3) & 0xff) << 24);
		default:
			throw new Error("invalid record type: " + mode(buf, offset));
		}
	}

	private static class Mode {
		final static byte BYTE = 'c';
		final static byte SHORT = 's';
		final static byte INT = 'l';
	}

	private static class Offset {
		final static int TYPE = 0; // byte
		final static int NFIELDS = 2; // short
		final static int SIZE = 4; // byte, short, or int <= type
	}

	public String toDebugString() {
		String s = "";
		s += "type: " + (char) mode(buf, offset) +
				" size: " + size() +
				" length: " + length();
//		for (int i = 0; i < Math.min(size(), 10); ++i)
//			System.out.println("offset " + i + ": " + getOffset(i));
		return s;
	}

	@Override
	public int nBufferable() {
		return 1;
	}

	@Override
	public int lengths(int[] lengths, int at) {
		lengths[at] = fieldOffset(buf, offset, -1);
		return 1;
	}

	public int length() {
		return fieldOffset(buf, offset, -1);
	}

	@Override
	public void addTo(ByteBuffer dst) {
		int n = length();
		for (int i = 0; i < n; ++i)
			dst.put(buf.get(offset + i));
	}

	public void addFieldTo(int fld, ByteBuffer dst) {
		// offset + getOffset(i), fieldLength(i)
		int from = offset + fieldOffset(buf, offset, fld);
		int lim = from + fieldLength(buf, offset, fld);
		for (int i = from; i < lim; ++i)
			dst.put(buf.get(i));
	}

	/**
	 * Will only work on in-memory records
	 * where buf was allocated with the correct length.
	 */
	public int persistRecord() {
		int len = length();
		int adr = Tran.mmf().alloc(len);
		ByteBuffer dst = Tran.mmf().buffer(adr);
		byte[] data = buf.array();
		assert len == data.length;
		dst.put(data);
		return adr;
	}

}
