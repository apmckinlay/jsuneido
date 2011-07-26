/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

import static suneido.SuException.unreachable;
import static suneido.SuException.verify;
import static suneido.util.Util.bufferUcompare;

import java.nio.ByteBuffer;
import java.util.Iterator;

import suneido.Packable;
import suneido.SuException;
import suneido.language.Ops;
import suneido.language.Pack;
import suneido.util.ByteBuf;

import com.google.common.collect.ImmutableList;

/**
 * Stores an array of {@link Packable} in a ByteBuf. Used by {@link Database}
 * to store records containing field values. Used by {@link Slots} for
 * {@link Btree} nodes. Provides a "view" onto a ByteBuf.
 * <p>
 * Format is:<br>
 * - one byte type = 'c', 's', 'l'<br>
 * - short n = number of fields<br>
 * - size (also referenced as offset[-1])<br>
 * - array of offsets<br>
 * size and array elements are of the type
 */
class Record implements suneido.intfc.database.Record, suneido.intfc.database.RecordBuilder {
	static final Record MINREC = new Record(5);
	static final Record MAXREC = new Record(8).addMax();
	static final ByteBuffer MIN_FIELD = ByteBuffer.allocate(0);
	static final ByteBuffer MAX_FIELD = ByteBuffer.allocate(1)
			.put(0, (byte) 0x7f).asReadOnlyBuffer();
	private Rep rep;
	private ByteBuf buf;
	private final long dboffset;
	private final boolean growable;

	private static class Type {
		static final byte BYTE = 'c';
		static final byte SHORT = 's';
		static final byte INT = 'l';
	}

	private static class Offset {
		static final int TYPE = 0; // byte
		static final int NFIELDS = 2; // short
		static final int SIZE = 4; // byte, short, or int <= type
	}

	Record() {
		rep = MINREC.rep;
		buf = MINREC.buf;
		dboffset = 0;
		growable = true;
	}

	/**
	 * Create a new BufRecord, allocating a new ByteBuf
	 *
	 * @param size
	 *            The required size, including both data and offsets
	 */
	Record(int size) {
		this(ByteBuf.allocate(size), size, true);
	}

	/**
	 * Create a new BufRecord in a supplied ByteBuf.
	 * @param size
	 *            The size of the buffer. Used to determine the required
	 *            representation.
	 */
	Record(ByteBuf buf, int size) {
		this(buf, size, false);
	}

	Record(ByteBuf buf, int size, boolean growable) {
		verify(size <= buf.size());
		this.buf = buf;
		setType(type(size));
		init();
		setSize(size);
		setNfields(0);
		dboffset = 0;
		this.growable = growable;
	}

	private static byte type(int size) {
		return size < 0x100 ? Type.BYTE : size < 0x10000 ? Type.SHORT
				: Type.INT;
	}

	/**
	 * Create a BufRecord on an existing ByteBuf in BufRecord format.
	 *
	 * @param buf
	 *            Must be in BufRecord format.
	 */
	Record(ByteBuf buf) {
		this.buf = buf;
		dboffset = 0;
		growable = false;
		init();
	}

	// position must be set correctly
	Record(ByteBuffer buf) {
		this(ByteBuf.wrap(buf));
	}

	Record(long dboffset, ByteBuffer buf) {
		this(dboffset, ByteBuf.wrap(buf));
	}

	Record(long dboffset, ByteBuf buf) {
		this.buf = buf;
		this.dboffset = dboffset;
		growable = false;
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
			throw new SuException("bad record type: " + getType());
		}
	}

	@Override
	public String toString() {
		if (getNfields() == 0)
			return "[]";
		if (equals(MAXREC))
			return "[MAX]";

		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < getNfields(); ++i)
			sb.append(getRaw(i).equals(MAX_FIELD) ? "MAX" : Ops.display(get(i)))
					.append(",");
		sb.deleteCharAt(sb.length() - 1).append("]");
		return sb.toString();
	}

	String toDebugString() {
		StringBuilder sb = new StringBuilder();
		sb.append(" limit ").append(buf.size()).append(" ");
		int nfields = getNfields();
		sb.append((char) getType()).append(" ").append(nfields).append(" = ");
		try {
			for (int i = 0; i < Math.max(nfields, 10); ++i)
				sb.append(rep.getOffset(i)).append(":").append(fieldSize(i)).append(" ");
		} catch (Throwable e) {
			sb.append(e);
		}
		return sb.toString();
	}

	void validate() {
		char type = (char) getType();
		assert type == 'c' || type == 's' || type == 'l';
		int nfields = getNfields();
		assert nfields >= 0;
		int limit = buf.size();
		for (int i = 0; i < nfields; ++i) {
			int offset = rep.getOffset(i);
			assert offset <= limit;
			int size = fieldSize(i);
			assert size >= 0;
			assert offset + size <= limit;
		}
	}

	long offset() {
		verify(dboffset != 0); // should only be called on database records
		return dboffset;
	}

	@Override
	public Record squeeze() {
		return (bufSize() > packSize()) ? dup() : this;
	}

	Record dup() {
		return dup(0);
	}

	Record dup(int extra) {
		Record dstRec = new Record(packSize() + extra);
		for (int i = 0; i < getNfields(); ++i)
			dstRec.add(buf, rep.getOffset(i), fieldSize(i));
		return dstRec;
	}

	// add's ========================================================

	Record add(ByteBuf src) {
		add(src, 0, src.size());
		return this;
	}

	@Override
	public Record add(ByteBuffer src) {
		int len = src.remaining();
		int dst = alloc(len);
		for (int i = 0; i < len; ++i)
			buf.put(dst++, src.get(src.position() + i));
		return this;
	}

	private Record add(ByteBuf src, int pos, int len) {
		int dst = alloc(len);
		for (int i = 0; i < len; ++i)
			buf.put(dst++, src.get(pos + i));
		return this;
	}

	@Override
	public Record add(long n) {
		int pos = alloc(Pack.packSizeLong(n));
		Pack.pack(n, buf.getByteBuffer(pos));
		return this;
	}

	@Override
	public Record add(Object x) {
		if (x == null)
			addMin();
		else {
			int pos = alloc(Pack.packSize(x));
			Pack.pack(x, buf.getByteBuffer(pos));
		}
		return this;
	}

	/** convenience method */
	Record addMmoffset(long n) {
		return add(Mmfile.offsetToInt(n));
	}

	@Override
	public Record addMin() {
		alloc(0);
		return this;
	}

	@Override
	public Record addMax() {
		int dst = alloc(1);
		buf.put(dst, (byte) 0x7f);
		return this;
	}

	@Override
	public Record addAll(suneido.intfc.database.Record r) {
		for (ByteBuffer buf : r)
			add(buf);
		return this;
	}

	/**
	 * Adds a new uninitialized field of the specified length and and sets the
	 * buffer position to it. Grows the buffer if required and the record is
	 * growable.
	 */
	int alloc(int len) {
		if (rep.avail() < len)
			grow(len);
		verify(len <= rep.avail());
		int n = getNfields();
		int offset = rep.getOffset(n - 1) - len;
		rep.setOffset(n, offset);
		setNfields(n + 1);
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

	boolean insert(int at, Object x) {
		int len = Pack.packSize(x);
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
		Pack.pack(x, buf.getByteBuffer(pos));
		setNfields(n + 1);
		return true;
	}

	int available() {
		return rep.avail();
	}

	private void moveLeft(int start, int end, int amount) {
		for (int i = start; i < end; ++i)
			buf.put(i - amount, buf.get(i));
	}

	void remove(int at) {
		remove(at, at + 1);
	}

	void remove(int begin, int end) {
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

	@Override
	public ByteBuffer getBuffer() {
		return buf.getByteBuffer(0, bufSize());
	}

	@Override
	public ByteBuffer getRaw(int i) {
		if (i >= getNfields())
			return MIN_FIELD;
		return buf.getByteBuffer(rep.getOffset(i), fieldSize(i));
	}

	ByteBuf getbuf(int i) {
		if (i >= getNfields())
			return ByteBuf.empty();
		return buf.slice(rep.getOffset(i), fieldSize(i));
	}

	@Override
	public Object get(int i) {
		// PERF could bypass getraw slice by setting/restoring position & limit
		return Pack.unpack(getRaw(i));
	}

	@Override
	public String getString(int i) {
		return Ops.toStr(get(i));
	}

	@Override
	public int getInt(int i) {
		// PERF could bypass getraw slice by setting/restoring position & limit
		return (Integer) Pack.unpack(getRaw(i));
	}

	short getShort(int i) {
		// PERF could bypass getraw slice by setting/restoring position & limit
		int n = (Integer) Pack.unpack(getRaw(i));
		assert (Short.MIN_VALUE <= n && n <= Short.MAX_VALUE);
		return (short) n;
	}

	long getMmoffset(int i) {
		return Mmfile.intToOffset(getInt(i));
	}

	boolean hasPrefix(Record r) {
		for (int i = 0; i < r.size(); ++i)
			if (!getRaw(i).equals(r.getRaw(i)))
				return false;
		return true;
	}

	boolean prefixgt(Record r) {
		int n = Math.min(size(), r.size());
		for (int i = 0; i < n; ++i) {
			int cmp = bufferUcompare(getRaw(i), r.getRaw(i));
			if (cmp != 0)
				return cmp > 0;
		}
		// prefix equal
		return false;
	}

	@Override
	public Record truncate(int n) {
		verify(n <= getNfields());
		setNfields(n);
		return this;
	}

	void truncateIfLarger(int n) {
		if (n < getNfields())
			setNfields(n);
	}

	@Override
	public int size() {
		return getNfields();
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	boolean allEmpty() {
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
	int fieldSize(int i) {
		if (i >= getNfields())
			return 0;
		return rep.getOffset(i - 1) - rep.getOffset(i);
	}

	@Override
	public int bufSize() {
		return getSize();
	}

	/**
	 * @param nfields
	 *            The number of fields.
	 * @param datasize
	 *            The total size of the field data.
	 * @return The minimum required buffer size.
	 */
	static int packSize(int nfields, int datasize) {
		int e = 1;
		int size = 2 /* type */+ 2 /* nfields */+ e /* size */+ nfields * e + datasize;
		if (size < 0x100)
			return size;
		e = 2;
		size = 2 /* type */+ 2 /* nfields */+ e /* size */+ nfields * e + datasize;
		if (size < 0x10000)
			return size;
		e = 4;
		return 2 /* type */+ 2 /* nfields */+ e /* size */+ nfields * e + datasize;
	}

	/**
	 * @return The minimum size the current data would fit into. <b>Note:</b>
	 *         This may be smaller than the current buffer size.
	 */
	@Override
	public int packSize() {
		int n = getNfields();
		int datasize = getSize() - rep.getOffset(n - 1);
		return packSize(n, datasize);
	}

	@Override
	public int packSize(int nest) {
		return packSize();
	}

	@Override
	public void pack(ByteBuffer dst) {
		int dstsize = packSize();
		if (getSize() == dstsize && dst.order() == buf.order())
			// already "compacted" so just bulk copy
			for (int i = 0; i < dstsize; ++i)
				dst.put(buf.get(i));
		else {
			// PERF do without allocating a temp record
			// maybe bulk copy then adjust like insert
			Record dstRec = new Record(ByteBuf.wrap(dst), dstsize);
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
		buf.putShortLE(Offset.NFIELDS, (short) nfields);
	}

	private short getNfields() {
		return buf.getShortLE(Offset.NFIELDS);
	}

	private void setSize(int sz) {
		rep.setOffset(-1, sz);
	}

	private int getSize() {
		return rep.getOffset(-1);
	}

	Record project(ImmutableList<Integer> fields) {
		return project(fields, 0);
	}

	Record project(ImmutableList<Integer> fields, long adr) {
		Record r = new Record();
		for (int i : fields)
			r.add(getbuf(i));
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
					- (2 /* type */+ 2 /* nfields */+ 1 /* byte */* (n + 2));
		}
	}

	private class ShortRep extends Rep {
		@Override
		void setOffset(int i, int sz) {
			buf.putShortLE(Offset.SIZE + 2 * (i + 1), (short) sz);
		}

		@Override
		int getOffset(int i) {
			return buf.getShortLE(Offset.SIZE + 2 * (i + 1)) & 0xffff;
		}

		@Override
		int avail() {
			int n = getNfields();
			return getOffset(n - 1)
					- (2 /* type */+ 2 /* nfields */+ 2 /* short */* (n + 2));
		}
	}

	private class IntRep extends Rep {
		@Override
		void setOffset(int i, int sz) {
			buf.putIntLE(Offset.SIZE + 4 * (i + 1), sz);
		}

		@Override
		int getOffset(int i) {
			return buf.getIntLE(Offset.SIZE + 4 * (i + 1));
		}

		@Override
		int avail() {
			int n = getNfields();
			return getOffset(n - 1)
					- (2 /* type */+ 2 /* nfields */+ 4 /* int */* (n + 2));
		}
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof Record))
			return false;
		return 0 == compareTo((Record) other);
	}

	@Override
	public int hashCode() {
		return buf.hashCode();
	}

	@Override
	public int compareTo(suneido.intfc.database.Record rec) {
		if (this == rec)
			return 0;
		int n = Math.min(size(), rec.size());
		for (int i = 0; i < n; ++i) {
			int cmp = compare1(i, (Record) rec);
			if (cmp != 0)
				return cmp;
		}
		return size() - rec.size();
	}

	private int compare1(int fld, Record rec) {
		int buf_i = rep.getOffset(fld);
		int recbuf_i = rec.rep.getOffset(fld);
		int n = Math.min(fieldSize(fld), rec.fieldSize(fld));
		for (int i = 0; i < n; ++i) {
			int cmp = (buf.get(buf_i++) & 0xff) - (rec.buf.get(recbuf_i++) & 0xff);
			if (cmp != 0)
				return cmp;
		}
		return fieldSize(fld) - rec.fieldSize(fld);
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
	public int address() {
		return Mmfile.offsetToInt(dboffset);
	}

	@Override
	public Object getRef() {
		// TODO use ByteBuffer instead of ByteBuf (less memory)
		return dboffset == 0 ? buf : dboffset;
	}

	@Override
	public Record build() {
		return this;
	}

}
