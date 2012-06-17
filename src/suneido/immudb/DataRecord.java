/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;

import com.google.common.primitives.Shorts;
import com.google.common.primitives.UnsignedInts;

/**
 * A BufRecord containing a data record (not just a key).
 * Tracks its address and tblnum as well as the data.
 */
class DataRecord extends BufRecord {
	private static final int TBLNUM_SIZE = Shorts.BYTES;
	private int address;
	private int tblnum;

	DataRecord(ByteBuffer buf) {
		super(buf);
	}

	DataRecord(Storage stor, int address) {
		super(stor.bufferBase(address), stor.bufferPos(address) + TBLNUM_SIZE);
		this.address = address;
		tblnum = buf.getShort(bufpos - TBLNUM_SIZE);
	}

	DataRecord(int address, ByteBuffer buf, int bufpos) {
		super(buf, bufpos);
		this.address = address;
	}

	int store(Storage stor) {
		assert 1 <= tblnum && tblnum < Short.MAX_VALUE : "invalid tblnum " + tblnum;
		address = stor.alloc(storSize());
		ByteBuffer buf = stor.buffer(address);
		buf.putShort((short) tblnum);
		pack(buf);
		return address;
	}

	int storSize() {
		return TBLNUM_SIZE + bufSize();
	}

	@Override
	public Object getRef() {
		return (address != 0) ? address : buf;
	}

	@Override
	public int address() {
		return address;
	}

	void address(int address) {
		this.address = address;
	}

	int tblnum() {
		return tblnum;
	}

	void tblnum(int tblnum) {
		assert this.tblnum == tblnum || this.tblnum == 0;
		this.tblnum = tblnum;
	}

	@Override
	public String toString() {
		String s = super.toString();
		if (address != 0)
			s += "@" + UnsignedInts.toString(address);
		return s;
	}

}
