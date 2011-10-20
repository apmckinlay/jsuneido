/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static com.google.common.base.Preconditions.checkState;
import static suneido.SuException.unreachable;

import java.nio.ByteBuffer;
import java.util.Iterator;

import suneido.language.Pack;

import com.google.common.base.Objects;

/**
 * Records stored in the database are prefixed with their table number.
 * The packed format, e.g. keys in Btree nodes, does NOT include table number.
 * Data is immutable, but tblnum is set after construction.
 */
class Record implements suneido.intfc.database.Record {
	static final Record EMPTY = new RecordBuilder().build();
	static final ByteBuffer EMPTY_BUF = ByteBuffer.allocate(0);
	/** Don't use zero for Mode, so zero memory is invalid
	 *  and so no overlap with Pack types */
	static class Mode {
		static final byte BYTE = 'c', SHORT = 's', INT = 'l'; }
	static class Offset {
		static final int MODE = 0, NFIELDS = 2, BODY = 4; }
	private final ByteBuffer buf;
	/** non-zero when the record is a key within a BtreeNode */
	private final int bufpos;
	/** used for stored data records */
	private final int address;
	/** used for stored data records
	 *  needed so you can update a record via just it's address */
	int tblnum;

	Record(ByteBuffer buf) {
		this(0, buf, 0);
	}

	Record(int address, ByteBuffer buf) {
		this(address, buf, 0);
	}

	Record(ByteBuffer buf, int bufpos) {
		this(0, buf, bufpos);
	}

	Record(int address, ByteBuffer buf, int bufpos) {
		this.buf = buf;
		this.bufpos = bufpos;
		this.address = address;
		check();
	}

	Record(Storage stor, int adr) {
		assert ! IntRefs.isIntRef(adr);
		buf = stor.buffer(adr);
		bufpos = TBLNUM_SIZE;
		this.address = adr;
		this.tblnum = (buf.get(0) & 0xff) + ((buf.get(1) & 0xff) << 8);
	}

	protected Record(Record rec) {
		this(rec.address, rec.buf, rec.bufpos);
	}

	Record(Record rec, int adr) {
		this(adr, rec.buf, rec.bufpos);
		tblnum = rec.tblnum;
	}

	void check() {
		assert bufpos >= 0;
		assert mode() != 0 : "invalid zero mode";
		assert bufSize() > 0 : "length " + bufSize();
		assert bufpos + bufSize() <= buf.capacity();
	}

	private int mode() {
		return buf.get(bufpos + Offset.MODE);
	}

	@Override
	public int size() {
		int si = bufpos + Offset.NFIELDS;
		return (buf.get(si) & 0xff) + ((buf.get(si + 1) & 0x3f) << 8);
	}

	ByteBuffer fieldBuffer(int i) {
		return buf;
	}

	int fieldLength(int i) {
		if (i >= size())
			return 0;
		return fieldOffset(i - 1) - fieldOffset(i);
	}

	// TODO use getShort and getInt
	int fieldOffset(int i) {
		// to match cSuneido use little endian (least significant first)
		switch (mode()) {
		case Mode.BYTE:
			return bufpos + (buf.get(bufpos + Offset.BODY + i + 1) & 0xff);
		case Mode.SHORT:
			int si = bufpos + Offset.BODY + 2 * (i + 1);
			return bufpos + ((buf.get(si) & 0xff) + ((buf.get(si + 1) & 0xff) << 8));
		case Mode.INT:
			int ii = bufpos + Offset.BODY + 4 * (i + 1);
			return bufpos + ((buf.get(ii) & 0xff) |
					((buf.get(ii + 1) & 0xff) << 8) |
			 		((buf.get(ii + 2) & 0xff) << 16) |
			 		((buf.get(ii + 3) & 0xff) << 24));
		default:
			throw new Error("invalid record type: " + mode());
		}
	}

	/** Number of bytes e.g. for storing */
	@Override
	public int bufSize() {
		return fieldOffset(-1) - bufpos;
	}

	private final int TBLNUM_SIZE = 2;

	int store(Storage stor) {
		checkState(1 <= tblnum && tblnum < Short.MAX_VALUE,
				"invalid tblnum %s", tblnum);
		int adr = stor.alloc(storSize());
		ByteBuffer buf = stor.buffer(adr);
		buf.put((byte) (tblnum & 0xff));
		buf.put((byte) (tblnum >> 8));
		pack(buf);
		return adr;
	}

	int storSize() {
		return TBLNUM_SIZE + bufSize();
	}

	@Override
	public void pack(ByteBuffer dst) {
		//PERF use array if available
		for (int i = 0; i < bufSize(); ++i)
			dst.put(buf.get(bufpos + i));
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
		for (int i = bufpos; i < bufpos + bufSize(); ++i)
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

	boolean startsWith(Record rec) {
		return prefixCompareTo(rec) == 0;
	}

	boolean prefixEquals(Record rec, int nfields) {
		if (rec.size() < nfields)
			return false;
		for (int i = 0; i < nfields; ++i)
			if (0 != compare1(this, rec, i))
				return false;
		return true;
	}

	/** @return true if this > rec
	 * comparing only as many fields as contained in rec
	 */
	boolean prefixGt(Record rec) {
		return prefixCompareTo(rec) > 0;
	}

	private int prefixCompareTo(Record that) {
		int n = that.size();
		for (int i = 0; i < n; ++i) {
			int cmp = compare1(this, that, i);
			if (cmp != 0)
				return cmp;
		}
		return 0;
	}

	private static int compare1(Record x, Record y, int i) {
		return compare1(
				x.fieldBuffer(i), x.fieldOffset(i), x.fieldLength(i),
				y.fieldBuffer(i), y.fieldOffset(i), y.fieldLength(i));
	}

	static int compare1(
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

	@Override
	public Object get(int i) {
		if (i >= size())
			return "";
		return get(fieldBuffer(i), fieldOffset(i), fieldLength(i));
	}

	static Object get(ByteBuffer buf, int off, int len) {
		if (len == 0)
			return "";
		byte x = buf.get(off);
		if (x == 'c' || x == 's' || x == 'l')
			return new Record(buf, off);
		ByteBuffer b = buf.duplicate();
		b.position(off);
		b.limit(off + len);
		return Pack.unpack(b);
		//PERF change unpack to take buf,i,n to eliminate duplicate
	}

	@Override
	public int getInt(int i) {
		long n = getLong(i);
		if (n < Integer.MIN_VALUE || Integer.MAX_VALUE < n)
			throw new RuntimeException("Record getInt value out of range");
		return (int) n;
	}

	long getLong(int i) {
		ByteBuffer b = buf.duplicate();
		int off = fieldOffset(i);
		b.position(off);
		b.limit(off + fieldLength(i));
		return Pack.unpackLong(b);
	}

	@Override
	public String getString(int i) {
		return (String) get(i);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < size(); ++i) {
			if (getRaw(i).equals(MAX_FIELD))
				sb.append("MAX");
			else {
				Object x = get(i);
				if (x instanceof String)
					sb.append("'").append(x).append("'");
				else if (x instanceof Number && ((Number) x).intValue() == IntRefs.MAXADR)
					sb.append("MAXADR");
				else
					sb.append(x);
			}
			sb.append(",");
		}
		if (sb.length() > 1)
			sb.deleteCharAt(sb.length() - 1);
		sb.append("]");
		return sb.toString();
	}

	String toDebugString() {
		Objects.ToStringHelper tsh = Objects.toStringHelper(this);
		tsh.add("type", mode())
			.add("size", size())
			.add("length", bufSize());
		for (int i = 0; i < Math.min(size(), 10); ++i)
			tsh.add("offset" + i, fieldOffset(i));
		return tsh.toString();
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
		return slice(bufpos, bufSize());
	}

	private ByteBuffer slice(int pos, int size) {
		ByteBuffer b = buf.duplicate();
		b.position(pos);
		b.limit(pos + size);
		return b.slice();
	}

	@Override
	public ByteBuffer getRaw(int i) {
		if (i >= size())
			return EMPTY_BUF;
		return slice(fieldOffset(i), fieldLength(i));
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public Object getRef() {
		if (address != 0)
			return address;
		if (bufpos == 0)
			return buf;
		ByteBuffer b = buf.duplicate();
		b.position(bufpos);
		return b.slice();
	}

	@Override
	public int address() {
		return address;
	}

	int prefixSize(int i) {
		assert 0 <= i && i <= size();
		return fieldOffset(-1) - fieldOffset(i - 1);
	}

}
