/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static com.google.common.base.Preconditions.checkState;
import static suneido.SuException.unreachable;

import java.nio.ByteBuffer;
import java.util.Iterator;

import suneido.language.Pack;

/**
 * Abstract base class with common code for BufRecord and ArrayRecord.
 * Records stored in the database are prefixed with their table number.
 * The packed format, e.g. keys in Btree nodes, does NOT include table number.
 * Mostly immutable. Constructed using RecordBuilder.
 */
abstract class Record implements suneido.intfc.database.Record {
	protected static final int TBLNUM_SIZE = 2;
	static final Record EMPTY = new RecordBuilder().build();
	static final ByteBuffer EMPTY_BUF = ByteBuffer.allocate(0);
	/** used for stored data records */
	int address;
	/** used for stored data records
	 *  needed so you can update a record via just it's address */
	int tblnum;

	protected Record() {
		this(0);
	}

	protected Record(int address) {
		this.address = address;
		tblnum = 0;
	}

	static Record from(ByteBuffer buf) {
		return new BufRecord(buf);
	}

	static Record from(int address, ByteBuffer buf) {
		return new BufRecord(address, buf);
	}

	static Record from(ByteBuffer buf, int bufpos) {
		return new BufRecord(0, buf, bufpos);
	}

	static Record from(int address, ByteBuffer buf, int bufpos) {
		return new BufRecord(address, buf, bufpos);
	}

	static Record from(Storage stor, int adr) {
		return new BufRecord(stor, adr);
	}

	abstract ByteBuffer fieldBuffer(int i);

	abstract int fieldLength(int i);

	abstract int fieldOffset(int i);

	/** Number of bytes e.g. for storing */
	@Override
	public int bufSize() {
		return packSize();
	}

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
	public int packSize(int nest) {
		return packSize();
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
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

	private static Object get(ByteBuffer buf, int off, int len) {
		if (len == 0)
			return "";
		byte x = buf.get(off);
		if (x == 'c' || x == 's' || x == 'l')
			return Record.from(buf, off);
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
		ByteBuffer b = fieldBuffer(i).duplicate();
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
			else if (i == size() - 1 && childRef() != null)
				sb.append("REF");
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
	public ByteBuffer getRaw(int i) {
		if (i >= size())
			return EMPTY_BUF;
		return slice(fieldBuffer(i), fieldOffset(i), fieldLength(i));
	}

	private static ByteBuffer slice(ByteBuffer buf, int pos, int size) {
		if (size == 0)
			return EMPTY_BUF;
		ByteBuffer b = buf.duplicate();
		if (pos == 0 && size == buf.limit())
			return b;
		b.position(pos);
		b.limit(pos + size);
		return b.slice();
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public Object getRef() {
		return (address != 0) ? address : getBuffer();
	}

	@Override
	public int address() {
		return address;
	}

	int prefixSize(int i) {
		assert 0 <= i && i <= size();
		int size = 0;
		for (--i; i >= 0; --i)
			size += fieldLength(i);
		return size;
	}

	Storable childRef() {
		return null;
	}

	Record withChildRef(Storable ref) {
		return new RecordBuilder().addAll(this).addRef(ref).build();
	}

}
