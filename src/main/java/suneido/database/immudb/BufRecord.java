/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

import com.google.common.base.MoreObjects;

import suneido.util.ByteBuffers;
import suneido.util.Immutable;

/**
 * A record stored in a ByteBuffer in the same format as cSuneido.
 * Used for records read from the database.
 * Also used to store keys within Btree nodes,
 * which are themselves stored as records.
 * @see DataRecord
 */
@Immutable
class BufRecord extends Record {
	static class Mode { static final byte BYTE = 1, SHORT = 2, INT = 3; }
	static class Offset { static final int BODY = 2; }
	protected final ByteBuffer buf;
	/** non-zero when the record is a key within a BtreeNode */
	protected final int bufpos;

	BufRecord(ByteBuffer buf) {
		this(buf, 0);
	}

	BufRecord(ByteBuffer buf, int bufpos) {
		this.buf = buf;
		this.bufpos = bufpos;
	}

	void check() {
		assert bufpos >= 0;
		assert mode() != 0 : "invalid zero mode";
		assert packSize() > 0 : "length " + packSize();
		assert bufpos + packSize() <= buf.capacity();
	}

	private int mode() {
		return (buf.get(bufpos) & 0xff) >>> 6;
	}

	/** Number of fields */
	@Override
	public int size() {
		if (buf.get(bufpos) == 0)
			return 0;
		return buf.getShort(bufpos) & 0x3fff;
	}

	@Override
	public ByteBuffer fieldBuffer(int i) {
		return buf;
	}

	@Override
	public int fieldLength(int i) {
		return fieldOffset(i - 1) - fieldOffset(i);
	}

	@Override
	public int fieldOffset(int i) {
		assert i >= -1;
		assert buf.get(bufpos) != 0;
		switch (mode()) {
		case Mode.BYTE:
			int bi = bufpos + Offset.BODY + i + 1;
			return bufpos + (buf.get(bi) & 0xff);
		case Mode.SHORT:
			int si = bufpos + Offset.BODY + 2 * (i + 1);
			return bufpos + (buf.getShort(si) & 0xffff);
		case Mode.INT:
			int ii = bufpos + Offset.BODY + 4 * (i + 1);
			return bufpos + buf.getInt(ii);
		default:
			throw new Error("invalid record type: " + mode());
		}
	}

	/** Number of bytes e.g. for storing */
	@Override
	public int packSize() {
		if (buf.get(bufpos) == 0)
			return 1;
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
		int hashCode = 17;
		for (int i = bufpos; i < bufpos + packSize(); ++i)
		      hashCode = 31 * hashCode + buf.get(i);
		return hashCode;
	}

	String toDebugString() {
		MoreObjects.ToStringHelper tsh = MoreObjects.toStringHelper(this);
		tsh.add("type", mode())
			.add("size", size())
			.add("length", packSize());
		for (int i = 0; i < Math.min(size(), 10); ++i)
			tsh.add("offset" + i, fieldOffset(i));
		return tsh.toString();
	}

	@Override
	public ByteBuffer getBuffer() {
		return ByteBuffers.slice(buf, bufpos, packSize());
	}

}
