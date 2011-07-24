/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.SuException.unreachable;

import java.nio.ByteBuffer;
import java.util.Iterator;

import suneido.language.Pack;

public class Record implements suneido.intfc.database.Record {
	public static Record EMPTY = new RecordBuilder().build();
	public static class Mode {
		public static final short BYTE = 1, SHORT = 2, INT = 3; }
	static class Offset {
		static final int HEADER = 0, BODY = 2; }
	private final ByteBuffer buf;
	private final int offset; // used when the record is a key within a BtreeNode

	public Record(ByteBuffer buf) {
		this(buf, 0);
	}

	public Record(ByteBuffer buf, int offset) {
		this.buf = buf;
		this.offset = offset;
		check();
	}

	public void check() {
		assert offset >= 0;
		assert mode() != 0;
		assert bufSize() > 0 : "length " + bufSize();
		assert offset < buf.capacity();
		assert offset + bufSize() <= buf.capacity()
			: "offset " + offset + " + length " + bufSize() + " > capacity " + buf.capacity();
	}

	private int mode() {
		return (buf.get(offset + Offset.HEADER + 1) & 0xff) >>> 6;
	}

	public int size() {
		int si = offset + Offset.HEADER;
		return (buf.get(si) & 0xff) + ((buf.get(si + 1) & 0x3f) << 8);
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
			return offset + (buf.get(offset + Offset.BODY + i + 1) & 0xff);
		case Mode.SHORT:
			int si = offset + Offset.BODY + 2 * (i + 1);
			return offset + ((buf.get(si) & 0xff) + ((buf.get(si + 1) & 0xff) << 8));
		case Mode.INT:
			int ii = offset + Offset.BODY + 4 * (i + 1);
			return offset + ((buf.get(ii) & 0xff) |
					((buf.get(ii + 1) & 0xff) << 8) |
			 		((buf.get(ii + 2) & 0xff) << 16) |
			 		((buf.get(ii + 3) & 0xff) << 24));
		default:
			throw new Error("invalid record type: " + mode());
		}
	}

	/** Number of bytes e.g. for storing */
	public int bufSize() {
		return fieldOffset(-1) - offset;
	}

	public int store(Storage stor) {
		int adr = stor.alloc(bufSize());
		ByteBuffer buf = stor.buffer(adr);
		pack(buf);
		return adr;
	}

	@Override
	public void pack(ByteBuffer dst) {
		// TODO use array if available
		for (int i = 0; i < bufSize(); ++i)
			dst.put(buf.get(offset + i));
	}

	@Override
	public int packSize(int nest) {
		return bufSize();
	}

	@Override
	public int packSize() {
		return packSize(0);
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
	public int hashCode() {
		int hashCode = 1;
		for (int i = offset; i < offset + bufSize(); ++i)
		      hashCode = 31 * hashCode + buf.get(i);
		return hashCode;
	}

	@Override
	public int compareTo(suneido.intfc.database.Record that) {
		int len1 = this.size();
		int len2 = that.size();
		int n = Math.min(len1, len2);
		for (int i = 0; i < n; ++i) {
			int cmp = compare1(this, (Record) that, i);
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

	public int getInt(int i) {
		// TODO avoid boxing
		return (Integer) get(i);
	}

	public long getLong(int i) {
		// TODO avoid boxing
		Object x = get(i);
		if (x instanceof Integer)
			return (Integer) x;
		else
			return (Long) x;
	}

	public String getString(int i) {
		return (String) get(i);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("<");
		for (int i = 0; i < size(); ++i) {
			Object x = get(i);
			/*if (x instanceof Integer)
				sb.append(Integer.toHexString(((Integer) x)));
			else*/ if (x instanceof String)
				sb.append("'").append(x).append("'");
			else
				sb.append(x);
			sb.append(",");
		}
		if (sb.length() > 1)
			sb.deleteCharAt(sb.length() - 1);
		sb.append(">");
		return sb.toString();
	}

	public String toDebugString() {
		String s = "";
		s += "type: " + (char) mode() +
				" size: " + size() +
				" length: " + bufSize();
		for (int i = 0; i < Math.min(size(), 10); ++i)
			System.out.println("offset " + i + ": " + fieldOffset(i));
		return s;
	}

	@Override
	public Iterator<ByteBuffer> iterator() {
		return new Iter();
	}

	private class Iter implements Iterator<ByteBuffer> {
		int i = 0;

		@Override
		public boolean hasNext() {
			return i < size();
		}

		@Override
		public ByteBuffer next() {
			return getRaw(i++);
		}

		@Override
		public void remove() {
			throw unreachable();
		}
	}

	@Override
	public Record squeeze() {
		return this;
	}

	@Override
	public ByteBuffer getBuffer() {
		return slice(offset, bufSize());
	}

	private ByteBuffer slice(int pos, int size) {
		ByteBuffer b = buf.duplicate();
		b.position(pos);
		b.limit(pos + size);
		return b.slice();
	}

	@Override
	public ByteBuffer getRaw(int i) {
		return slice(fieldOffset(i), fieldLength(i));
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public Object getRef() {
		// TODO Auto-generated method stub
		return null;
	}

}
