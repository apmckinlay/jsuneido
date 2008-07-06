package suneido.database;

import static suneido.Suneido.verify;

import java.nio.ByteBuffer;

import suneido.Packable;
import suneido.SuException;
import suneido.SuInteger;
import suneido.SuNumber;
import suneido.SuString;
import suneido.SuValue;

/**
 * Stores an array of {@link Packable} in a ByteBuffer. Used by {@link Database}
 * to store records containing field values. Used by {@link Slots} for
 * {@link Btree} nodes. Provides a "view" onto a ByteBuffer.
 * <p>
 * Format is:<br>
 * - one byte type = 'c', 's', 'l'<br>
 * - short n = number of fields<br>
 * - size (also referenced as offset[-1])<br>
 * - array of offsets<br>
 * size and array elements are of the type
 *
 * @author Andrew McKinlay
 *         <p>
 *         <small>Copyright 2008 Suneido Software Corp. All rights reserved.
 *         Licensed under GPLv2.</small>
 *         </p>
 */
public class Record implements suneido.Packable, Comparable<Record> {
	public final static Record MINREC = new Record(4);
	public final static Record MAXREC = new Record(7).addMax();
	private Rep rep;
	private ByteBuffer buf;
	private long dboffset = 0;
	private boolean growable = false;

	private static class Type {
		final static byte BYTE = 'c';
		final static byte SHORT = 's';
		final static byte INT = 'l';
	}

	private static class Offset {
		final static int TYPE = 0; // byte
		final static int NFIELDS = 1; // short
		final static int SIZE = 3; // byte, short, or int <= type
	}

	public Record() {
		rep = MINREC.rep;
		buf = MINREC.buf;
		growable = true;
	}

	/**
	 * Create a new BufRecord, allocating a new ByteBuffer
	 *
	 * @param size
	 *            The required size, including both data and offsets
	 */
	public Record(int size) {
		this(ByteBuffer.allocate(size), size);
		growable = true;
	}

	/**
	 * Create a new BufRecord using a supplied ByteBuffer.
	 *
	 * @param buf
	 * @param size
	 *            The size of the buffer. Used to determine the required
	 *            representation.
	 */
	public Record(ByteBuffer buf, int size) {
		verify(size <= buf.limit());
		this.buf = buf;
		setType(type(size));
		init();
		setSize(size);
		setNfields(0);
	}

	private static byte type(int size) {
		return size < 0x100 ? Type.BYTE : size < 0x10000 ? Type.SHORT
				: Type.INT;
	}

	/**
	 * Create a BufRecord on an existing ByteBuffer in BufRecord format.
	 *
	 * @param buf
	 *            Must be in BufRecord format.
	 */
	public Record(ByteBuffer buf) {
		this.buf = buf;
		init();
	}

	public Record(ByteBuffer buf, long dboffset) {
		this.buf = buf;
		this.dboffset = dboffset;
		init();
	}

	private void init() {
		switch (getType()) {
		case Type.BYTE:
			rep = new ByteRep();
			break;
		case Type.SHORT:
			rep = new ShortRep();
			break;
		case Type.INT:
			rep = new IntRep();
			break;
		default:
			throw SuException.unreachable();
		}
	}

	@Override
	public String toString() {
		if (getNfields() == 0)
			return "[]";
		if (equals(MAXREC))
			return "[MAX]";
		String s = "[";
		for (int i = 0; i < getNfields(); ++i)
			s += get(i) + ",";
		return s.substring(0, s.length() - 1) + "]";
	}

	public Object toObject() {
		return dboffset == 0 ? array() : Integer.valueOf(Mmfile
				.offsetToInt(dboffset));
	}

	private byte[] array() {
		byte[] array;
		int bufSize = bufSize();
		if (buf.hasArray() && buf.arrayOffset() == 0
				&& (array = buf.array()).length == bufSize)
			return array;
		array = new byte[bufSize];
		buf.get(array, 0, bufSize);
		return array;
	}

	public static Record fromObject(Mmfile mmf, Object ob) {
		return ob instanceof Integer ? new Record(mmf.adr(Mmfile
				.intToOffset((Integer) ob))) : new Record(ByteBuffer
						.wrap((byte[]) ob));
	}

	public long off() {
		verify(dboffset != 0); // should only be called on database records
		return dboffset;
	}

	public Record dup() {
		return dup(0);
	}

	public Record dup(int extra) {
		Record dstRec = new Record(packSize() + extra);
		for (int i = 0; i < getNfields(); ++i)
			dstRec.add(buf, rep.getOffset(i), fieldSize(i));
		return dstRec;
	}

	// add's ========================================================

	public Record add(ByteBuffer src) {
		add(src, 0, src.limit());
		return this;
	}

	public Record add(ByteBuffer src, int pos, int len) {
		alloc(len);
		for (int i = 0; i < len; ++i)
			buf.put(src.get(pos + i));
		return this;
	}

	public Record add(Packable x) {
		alloc(x.packSize());
		x.pack(buf);
		return this;
	}

	/** convenience method */
	public Record add(String s) {
		// PERF could bypass SuString instance
		return add(new SuString(s));
	}

	/** convenience method */
	public Record add(int x) {
		// PERF could bypass SuInteger instance
		return add(SuInteger.valueOf(x));
	}

	/** convenience method */
	public Record addMmoffset(long n) {
		return add(Mmfile.offsetToInt(n));
	}

	public Record addMin() {
		alloc(0);
		return this;
	}

	public Record addMax() {
		alloc(1);
		buf.put((byte) 0x7f);
		return this;
	}

	/**
	 * Adds a new uninitialized field of the specified length and and sets the
	 * buffer position to it. Grows the buffer if required and the record is
	 * growable.
	 */
	public int alloc(int len) {
		if (rep.avail() < len)
			grow(len);
		verify(len <= rep.avail());
		int n = getNfields();
		int offset = rep.getOffset(n - 1) - len;
		rep.setOffset(n, offset);
		setNfields(n + 1);
		buf.position(offset);
		return offset;
	}

	private void grow(int needed) {
		verify(growable);
		Record tmp = new Record(Math.max(64, 2 * (bufSize() + needed)));
		for (int i = 0; i < getNfields(); ++i)
			tmp.add(buf, rep.getOffset(i), fieldSize(i));
		buf = tmp.buf;
		rep = tmp.rep;
	}

	public boolean insert(int at, Packable x) {
		int len = x.packSize();
		if (len > rep.avail())
			return false;
		int n = getNfields();
		// insert into heap
		moveLeft(rep.getOffset(n - 1), rep.getOffset(at - 1), len);
		// insert into offsets
		// adjust offsets after it (after because heap grows down)
		rep.insert1(at, n, len);
		int pos = rep.getOffset(at - 1) - len;
		rep.setOffset(at, pos);
		buf.position(pos);
		x.pack(buf);
		setNfields(n + 1);
		return true;
	}

	private void moveLeft(int start, int end, int amount) {
		for (int i = start; i < end; ++i)
			buf.put(i - amount, buf.get(i));
	}

	public void remove(int at) {
		remove(at, at + 1);
	}

	public void remove(int begin, int end) {
		int n = getNfields();
		verify(begin <= end && 0 <= begin && end <= n);
		int len = rep.getOffset(begin - 1) - rep.getOffset(end - 1);
		moveRight(rep.getOffset(n - 1), rep.getOffset(end - 1), len);
		rep.remove(n, begin, end, len);
		setNfields(n - (end - begin));
	}

	private void moveRight(int start, int end, int amount) {
		for (int i = end - 1; i >= start; --i)
			buf.put(i + amount, buf.get(i));
	}

	// get's ========================================================

	private final static ByteBuffer EMPTY_BUF = ByteBuffer.allocate(0);

	public ByteBuffer getraw(int i) {
		if (i >= getNfields())
			return EMPTY_BUF;
		buf.position(rep.getOffset(i));
		ByteBuffer result = buf.slice();
		result.limit(fieldSize(i));
		return result;
	}

	public SuValue get(int i) {
		return SuValue.unpack(getraw(i));
	}

	public String getString(int i) {
		// PERF could bypass SuValue instance if SuString
		return get(i).string();
	}

	public long getLong(int i) {
		return SuNumber.unpackLong(getraw(i));
	}

	public int getInt(int i) {
		long n = SuNumber.unpackLong(getraw(i));
		assert (Integer.MIN_VALUE <= n && n <= Integer.MAX_VALUE);
		return (int) n;
	}

	public short getShort(int i) {
		long n = SuNumber.unpackLong(getraw(i));
		assert (Short.MIN_VALUE <= n && n <= Short.MAX_VALUE);
		return (short) n;
	}

	public long getMmoffset(int i) {
		return Mmfile.intToOffset(getInt(i));
	}

	public boolean hasPrefix(Record r) {
		for (int i = 0; i < r.size(); ++i)
			if (!getraw(i).equals(r.getraw(i)))
				return false;
		return true;
	}

	public boolean prefixgt(Record r) {
		int n = Math.min(size(), r.size());
		for (int i = 0; i < n; ++i) {
			int cmp = getraw(i).compareTo(r.getraw(i));
			if (cmp > 0)
				return cmp > 0;
		}
		// prefix equal
		return false;
	}

	public Record truncate(int n) {
		verify(n <= getNfields());
		setNfields(n);
		return this;
	}

	/**
	 * @return The number of fields in the BufRecord.
	 */
	public int size() {
		return getNfields();
	}

	public boolean isEmpty() {
		return size() == 0;
	}

	public boolean allEmpty() {
		for (int i = size() - 1; i >= 0; --i)
			if (fieldSize(i) != 0)
				return false;
		return true;
	}

	/**
	 * @param i
	 *            The index of the field to get the size of.
	 * @return The size of the i'th field.
	 */
	public int fieldSize(int i) {
		if (i >= getNfields())
			return 0;
		return rep.getOffset(i - 1) - rep.getOffset(i);
	}

	/**
	 * @return The current buffer size. May be larger than the packsize.
	 */
	public int bufSize() {
		return getSize();
	}

	/**
	 * Used by MemRecord
	 *
	 * @param nfields
	 *            The number of fields.
	 * @param datasize
	 *            The total size of the field data.
	 * @return The minimum required buffer size.
	 */
	public static int packSize(int nfields, int datasize) {
		int e = 1;
		int size = 1 /* type */+ 2 /* nfields */+ e /* size */+ nfields * e
		+ datasize;
		if (size < 0x100)
			return size;
		e = 2;
		size = 1 /* type */+ 2 /* nfields */+ e /* size */+ nfields * e + datasize;
		if (size < 0x10000)
			return size;
		e = 4;
		return 1 /* type */+ 2 /* nfields */+ e /* size */+ nfields * e + datasize;
	}

	/**
	 * @return The minimum size the current data would fit into. <b>Note:</b>
	 *         This may be smaller than the current buffer size.
	 */
	public int packSize() {
		int n = getNfields();
		int datasize = getSize() - rep.getOffset(n - 1);
		return packSize(n, datasize);
	}

	public void pack(ByteBuffer dst) {
		int dstsize = packSize();
		if (getSize() == dstsize)
			// already "compacted" so just bulk copy
			for (int i = 0; i < dstsize; ++i)
				dst.put(buf.get(i));
		else {
			// PERF do without allocating a temp record
			// maybe bulk copy then adjust like insert
			Record dstRec = new Record(dst.slice(), dstsize);
			for (int i = 0; i < getNfields(); ++i)
				dstRec.add(buf, rep.getOffset(i), fieldSize(i));
			dst.position(dst.position() + dstsize);
		}
	}

	private void setType(byte t) {
		buf.put(Offset.TYPE, t);
	}

	private byte getType() {
		return buf.get(Offset.TYPE);
	}

	private void setNfields(int nfields) {
		buf.putShort(Offset.NFIELDS, (short) nfields);
	}

	private short getNfields() {
		return buf.getShort(Offset.NFIELDS);
	}

	private void setSize(int sz) {
		rep.setOffset(-1, sz);
	}

	private int getSize() {
		return rep.getOffset(-1);
	}

	public Record project(short[] fields) {
		return project(fields, 0);
	}
	public Record project(short[] fields, long adr) {
		Record r = new Record();
		for (int i : fields)
			r.add(getraw(i));
		if (adr != 0)
			r.addMmoffset(adr);
		return r;
	}

	// "strategy" object to avoid switching on type.
	private abstract class Rep {
		abstract void setOffset(int i, int offset);

		abstract int getOffset(int i);

		abstract int avail(); // allows for new items offset

		private void insert1(int start, int end, int adjust) {
			for (int i = end; i > start; --i)
				setOffset(i, getOffset(i - 1) - adjust);
		}

		private void remove(int n, int start, int end, int adjust) {
			for (int i = start; end < n; ++i)
				setOffset(i, getOffset(end++) + adjust);
		}
	}

	private class ByteRep extends Rep {
		@Override
		void setOffset(int i, int sz) {
			buf.put(Offset.SIZE + i + 1, (byte) sz);
		}

		@Override
		int getOffset(int i) {
			return buf.get(Offset.SIZE + i + 1) & 0xff;
		}

		@Override
		int avail() {
			int n = getNfields();
			return getOffset(n - 1)
			- (1 /* type */+ 2 /* nfields */+ 1 /* byte */* (n + 2));
		}
	}

	private class ShortRep extends Rep {
		@Override
		void setOffset(int i, int sz) {
			buf.putShort(Offset.SIZE + 2 * (i + 1), (short) sz);
		}

		@Override
		int getOffset(int i) {
			return buf.getShort(Offset.SIZE + 2 * (i + 1)) & 0xffff;
		}

		@Override
		int avail() {
			int n = getNfields();
			return getOffset(n - 1)
			- (1 /* type */+ 2 /* nfields */+ 2 /* short */* (n + 2));
		}
	}

	private class IntRep extends Rep {
		@Override
		void setOffset(int i, int sz) {
			buf.putInt(Offset.SIZE + 4 * (i + 1), sz);
		}

		@Override
		int getOffset(int i) {
			return buf.getInt(Offset.SIZE + 4 * (i + 1));
		}

		@Override
		int avail() {
			int n = getNfields();
			return getOffset(n - 1)
			- (1 /* type */+ 2 /* nfields */+ 4 /* int */* (n + 2));
		}
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Record ? 0 == compareTo((Record) other) : false;
	}

	public int compareTo(Record rec) {
		if (this == rec)
			return 0;
		int n = Math.min(size(), rec.size());
		for (int i = 0; i < n; ++i) {
			int cmp = compare1(i, rec);
			if (cmp != 0)
				return cmp;
		}
		return size() - rec.size();
	}

	private int compare1(int fld, Record rec) {
		buf.position(rep.getOffset(fld));
		rec.buf.position(rec.rep.getOffset(fld));
		int n = Math.min(fieldSize(fld), rec.fieldSize(fld));
		for (int i = 0; i < n; ++i) {
			int cmp = (buf.get() & 0xff) - (rec.buf.get() & 0xff);
			if (cmp != 0)
				return cmp;
		}
		return fieldSize(fld) - rec.fieldSize(fld);
	}

	public boolean inRange(Record from, Record to) {
		return compareTo(from) >= 0 && compareTo(to) <= 0;
	}
}
