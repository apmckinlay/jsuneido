/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

import suneido.Packable;
import suneido.language.Pack;

public class Record implements Comparable<Record>, Packable {
	public static Record EMPTY = new RecordBuilder().build();
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
	private final int offset; // used to point to a key in a BtreeNode

	public Record(ByteBuffer buf, int offset) {
		this.buf = buf;
		this.offset = offset;
		check();
	}

	public void check() {
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

	public int size() {
		int si = offset + Offset.NFIELDS;
		return (buf.get(si) & 0xff) + ((buf.get(si + 1) & 0xff) << 8);
	}

	public ByteBuffer fieldBuffer(int i) {
		return buf;
	}

	public int fieldLength(int i) {
		if (i >= size())
			return 0;
		return fieldOffset(i - 1) - fieldOffset(i);
	}

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

	public int length() {
		return fieldOffset(-1) - offset;
	}

	public int store(Storage stor) {
		int adr = stor.alloc(length());
		ByteBuffer buf = stor.buffer(adr);
		pack(buf);
		return adr;
	}

	@Override
	public void pack(ByteBuffer dst) {
		// TODO use array if available
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

	public int packSize(int nest) {
		return length();
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
		int len1 = this.size();
		int len2 = that.size();
		int n = Math.min(len1, len2);
		for (int i = 0; i < n; ++i) {
			int cmp = compare1(this, that, i);
			if (cmp != 0)
				return cmp;
		}
		return len1 - len2;
	}

	public boolean startsWith(Record rec) {
		int n = rec.size();
		if (n > size())
			return false;
		return prefixEquals(rec, n);
	}

	public boolean prefixEquals(Record rec, int nfields) {
		for (int i = 0; i < nfields; ++i)
			if (0 != compare1(this, rec, i))
				return false;
		return true;
	}

	private static int compare1(Record x, Record y, int i) {
		return compare1(
				x.fieldBuffer(i), x.fieldOffset(i), x.fieldLength(i),
				y.fieldBuffer(i), y.fieldOffset(i), y.fieldLength(i));
	}

	public static int compare1(
			ByteBuffer buf1, int off1, int len1,
			ByteBuffer buf2, int off2, int len2) {
		int n = Math.min(len1, len2);
		for (int i = 0; i < n; ++i) {
			int cmp = (buf1.get(off1 + i) & 0xff) - (buf2.get(off2 + i) & 0xff);
			if (cmp != 0)
				return cmp;
		}
		return len1 - len2;
	}

	public Object get(int i) {
		if (i >= size())
			return "";
		return get(fieldBuffer(i), fieldOffset(i), fieldLength(i));
	}

	public static Object get(ByteBuffer buf, int off, int len) {
		if (len == 0)
			return "";
		byte x = buf.get(off);
		if (x == 'c' || x == 's' || x == 'l')
			return new Record(buf, off);
		ByteBuffer b = buf.duplicate();
		b.position(off);
		b.limit(off + len);
		return Pack.unpack(b);
		// TODO change unpack to take buf,i,n to eliminate duplicate
	}

	@Override
	public String toString() {
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
		if (sb.length() > 1)
			sb.deleteCharAt(sb.length() - 1);
		sb.append(">");
		return sb.toString();
	}

}
