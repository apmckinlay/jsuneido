/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Objects;

/**
 * A record stored in a ByteBuffer in the same format as cSuneido.
 * Used for records read from the database.
 * Also used to store keys within Btree nodes,
 * which themselves are stored as records.
 */
@Immutable
class BufRecord extends Record {
	static class Mode { static final byte BYTE = 'c', SHORT = 's', INT = 'l'; }
	static class Offset { static final int MODE = 0, NFIELDS = 2, BODY = 4; }
	private final ByteBuffer buf;
	/** non-zero when the record is a key within a BtreeNode */
	private final int bufpos;

	BufRecord(ByteBuffer buf) {
		this(0, buf, 0);
	}

	BufRecord(ByteBuffer buf, int bufpos) {
		this(0, buf, bufpos);
	}

	BufRecord(int address, ByteBuffer buf) {
		this(address, buf, 0);
	}

	BufRecord(Storage stor, int adr) {
		this(adr, stor.buffer(adr), TBLNUM_SIZE);
		tblnum = buf.getShort(0);
	}

	BufRecord(int address, ByteBuffer buf, int bufpos) {
		super(address);
		this.buf = buf;
		this.bufpos = bufpos;
	}

	protected BufRecord(BufRecord rec) {
		this(rec.buf, rec.bufpos);
	}

	void check() {
		assert bufpos >= 0;
		assert mode() != 0 : "invalid zero mode";
		assert packSize() > 0 : "length " + packSize();
		assert bufpos + packSize() <= buf.capacity();
	}

	private int mode() {
		return buf.get(bufpos + Offset.MODE);
	}

	@Override
	public int size() {
		int si = bufpos + Offset.NFIELDS;
		return (buf.get(si) & 0xff) + ((buf.get(si + 1) & 0x3f) << 8);
	}

	@Override
	public ByteBuffer fieldBuffer(int i) {
		return buf;
	}

	@Override
	public int fieldLength(int i) {
		return fieldOffset(i - 1) - fieldOffset(i);
	}

	// TODO use getShort and getInt
	@Override
	public int fieldOffset(int i) {
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
	public int packSize() {
		return fieldOffset(-1) - bufpos;
	}

	@Override
	public void pack(ByteBuffer dst) {
		//PERF use array if available
		for (int i = 0; i < packSize(); ++i)
			dst.put(buf.get(bufpos + i));
	}

	@Override
	public int hashCode() {
		int hashCode = 1;
		for (int i = bufpos; i < bufpos + packSize(); ++i)
		      hashCode = 31 * hashCode + buf.get(i);
		return hashCode;
	}

	String toDebugString() {
		Objects.ToStringHelper tsh = Objects.toStringHelper(this);
		tsh.add("type", mode())
			.add("size", size())
			.add("length", packSize());
		for (int i = 0; i < Math.min(size(), 10); ++i)
			tsh.add("offset" + i, fieldOffset(i));
		return tsh.toString();
	}

	@Override
	public ByteBuffer getBuffer() {
		ByteBuffer b = buf.duplicate();
		b.position(bufpos);
		b.limit(bufpos + packSize());
		return b.slice();
	}

}
