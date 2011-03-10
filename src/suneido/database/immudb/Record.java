/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.Immutable;

import suneido.language.Pack;

/**
 * A list of values stored in a ByteBuffer in the same format as cSuneido.
 * Values are stored using {@link suneido.language.Pack}
 * @see RecordBuilder
 */
@Immutable
public class Record implements Comparable<Record> {
	protected static final ByteBuffer emptyRecBuf = new RecordBuilder().asByteBuffer();
	public static final Record EMPTY = new Record();
	public final ByteBuffer buf;
	public final int offset;

	protected Record() {
		this(emptyRecBuf, 0);
	}

	public Record(ByteBuffer buf, int offset) {
		this.buf = buf;
		this.offset = offset;
		assert offset >= 0;
		assert mode() == 'c' || mode() == 's' || mode() == 'l';
		assert size() >= 0;
		assert length() > 0 : length();
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

	public int length() {
		return length(buf, offset);
	}
	public static int length(ByteBuffer buf, int offset) {
		return fieldOffset(buf, offset, -1) - offset;
	}

	public void addFieldTo(int fld, ByteBuffer dst) {
		// offset + getOffset(i), fieldLength(i)
		int from = fieldOffset(buf, offset, fld);
		int lim = from + fieldLength(buf, offset, fld);
		for (int i = from; i < lim; ++i)
			dst.put(buf.get(i));
	}

	/**
	 * Will only work on in-memory records
	 * where buf was allocated with the correct length.
	 */
	public int storeRecord(Storage stor) {
		int len = length();
		int adr = stor.alloc(len);
		ByteBuffer dst = stor.buffer(adr);
		byte[] data = buf.array();
		assert len == data.length;
		dst.put(data);
		return adr;
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

	public Object get(int i) {
		return get(buf, offset, i);
	}
	public static Object get(ByteBuffer buf, int offset, int i) {
		if (i >= size(buf, offset) || fieldLength(buf, offset, i) == 0)
			return "";
		int pos = fieldOffset(buf, offset, i);
		byte x = buf.get(pos);
		if (x == 'c' || x == 's' || x == 'l')
			return new Record(buf, pos);
		ByteBuffer b = buf.duplicate();
		b.position(pos);
		b.limit(pos + fieldLength(buf, offset, i));
		return Pack.unpack(b);
		// TODO change unpack to take buf,i,n to eliminate duplicate
	}

	@Override
	public boolean equals(Object that) {
		if (this == that)
			return true;
		if (that instanceof Record)
			return 0 == compareTo((Record) that);
		return false;
	}

	@Override
	public int compareTo(Record that) {
		return compare(this.buf, this.offset, that.buf, that.offset);
	}

	public static int compare(
			ByteBuffer buf, int offset,
			ByteBuffer buf2, int offset2) {
		int len1 = size(buf, offset);
		int len2 = size(buf2, offset2);
		int n = Math.min(len1, len2);
		for (int i = 0; i < n; ++i) {
			int cmp = compare1(
					buf, fieldOffset(buf, offset, i), fieldLength(buf, offset, i),
					buf2, fieldOffset(buf2, offset2, i), fieldLength(buf2, offset2, i));
			if (cmp != 0)
				return cmp;
		}
		return len1 - len2;
	}

	public static int compare1(
			ByteBuffer buf1, int idx1, int len1,
			ByteBuffer buf2, int idx2, int len2) {
		int n = Math.min(len1, len2);
		for (int i = 0; i < n; ++i) {
			int cmp = (buf1.get(idx1 + i) & 0xff) - (buf2.get(idx2 + i) & 0xff);
			if (cmp != 0)
				return cmp;
		}
		return len1 - len2;
	}

	public boolean startsWith(Record rec) {
		int n = rec.size();
		if (n > size())
			return false;
		for (int i = 0; i < n; ++i)
			if (0 != compare1(
					buf, fieldOffset(i), fieldLength(i),
					rec.buf, rec.fieldOffset(i), rec.fieldLength(i)))
				return false;
		return true;
	}

	@Override
	public String toString() {
//System.out.println("mode " + (char) mode() + " nfields " + size() + " length " + length());
//for (int i = 0; i < size(); ++i) {
//System.out.print("    offset " + fieldOffset(i) + " length " + fieldLength(i));
//if (fieldLength(i) > 0)
//System.out.print(" first byte " + (buf.get(fieldOffset(i)) & 0xff));
//System.out.println();
//}
		StringBuilder sb = new StringBuilder();
		sb.append("<");
		for (int i = 0; i < size(); ++i) {
			Object x = get(i);
			if (x instanceof Integer)
				sb.append(Integer.toHexString(((Integer) x)));
			else
				sb.append(x);
			sb.append(",");
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(">");
		return sb.toString();
	}

}
