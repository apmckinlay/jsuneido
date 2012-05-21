/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import gnu.trove.list.array.TIntArrayList;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import suneido.immudb.BufRecord.Mode;
import suneido.language.Pack;

/**
 * The result of RecordBuilder.
 * Certain operations will convert on-demand to a {@link BufRecord}
 * Mostly immutable, except for bufrec (cache) and setLast
 */
class ArrayRecord extends Record {
	private final ArrayList<ByteBuffer> bufs;
	private final TIntArrayList offs;
	private final TIntArrayList lens;
	private BufRecord bufrec;

	ArrayRecord(ArrayList<ByteBuffer> bufs, TIntArrayList offs, TIntArrayList lens) {
		assert offs.size() == bufs.size();
		assert lens.size() == bufs.size();
		this.bufs = bufs;
		this.offs = offs;
		this.lens = lens;
	}

	@Override
	public int size() {
		return bufs.size();
	}

	@Override
	public ByteBuffer fieldBuffer(int i) {
		return bufs.get(i);
	}

	@Override
	public int fieldOffset(int i) {
		return offs.get(i);
	}

	@Override
	public int fieldLength(int i) {
		return lens.get(i);
	}

	@Override
	public void pack(ByteBuffer dst) {
		if (bufrec != null)
			bufrec.pack(dst);
		else
			pack(dst, packSize());
	}

	@Override
	public ByteBuffer getBuffer() {
		return bufRecord().getBuffer();
	}

	@Override
	public Record squeeze() {
		return bufRecord();
	}

	void setLast(int adr) {
		ByteBuffer buf = Pack.packLong(adr & 0xffffffffL);
		bufs.set(size() - 1, buf);
		offs.set(size() - 1, 0);
		lens.set(size() - 1, buf.remaining());
	}

	// convert to BufRecord ----------------------------------------------------

	private BufRecord bufRecord() {
		if (bufrec == null) {
			int length = packSize();
			ByteBuffer buf = ByteBuffer.allocate(length);
			pack(buf, length);
			bufrec = new BufRecord(buf);
		}
		return bufrec;
	}

	@Override
	public int packSize() {
		int nfields = bufs.size();
		int datasize = 0;
		for (int i = 0; i < nfields; ++i)
			datasize += lens.get(i);
		return length(nfields, datasize);
	}

	static int length(int nfields, int datasize) {
		// Mode.BYTE
		int length = 4 + (1 + nfields) + datasize;
		if (length < 0x100)
			return length;
		// Mode.SHORT
		length = 4 + 2 * (1 + nfields) + datasize;
		if (length < 0x10000)
			return length;
		// Mode.INT
		return 4 + 4 * (1 + nfields) + datasize;
	}

	private void pack(ByteBuffer dst, int length) {
		packHeader(dst, length, lens);
		int nfields = bufs.size();
		for (int i = nfields - 1; i >= 0; --i)
			pack1(dst, bufs.get(i), offs.get(i), lens.get(i));
	}

	static void packHeader(ByteBuffer dst, int length, TIntArrayList lens) {
		dst.order(ByteOrder.LITTLE_ENDIAN); // to match cSuneido format
		byte mode = mode(length);
		dst.put(mode);
		dst.put((byte) 0);
		int nfields = lens.size();
		assert 0 <= nfields && nfields <= Short.MAX_VALUE;
		dst.putShort((short) nfields);
		packOffsets(dst, length, lens, mode);
		dst.order(ByteOrder.BIG_ENDIAN);
	}
	private static byte mode(int length) {
		if (length < 0x100)
			return Mode.BYTE;
		else if (length < 0x10000)
			return Mode.SHORT;
		else
			return Mode.INT;
	}
	private static void packOffsets(ByteBuffer dst, int length,
			TIntArrayList lens, int mode) throws Error {
		int nfields = lens.size();
		int offset = length;
		assert length > 0;
		switch (mode) {
		case Mode.BYTE:
			dst.put((byte) offset);
			for (int i = 0; i < nfields; ++i)
				dst.put((byte) (offset -= lens.get(i)));
			break;
		case Mode.SHORT:
			dst.putShort((short) offset);
			for (int i = 0; i < nfields; ++i)
				dst.putShort((short) (offset -= lens.get(i)));
			break;
		case Mode.INT:
			dst.putInt(offset);
			for (int i = 0; i < nfields; ++i)
				dst.putInt(offset -= lens.get(i));
			break;
		default:
			throw new Error("bad record mode: " + mode);
		}
	}

	private static void pack1(ByteBuffer dst, ByteBuffer buf, int off, int len) {
		if (buf.hasArray()) {
			byte[] a = buf.array();
			off += buf.arrayOffset();
			dst.put(a, off, len);
		} else {
			for (int i = 0; i < len; ++i)
				dst.put(buf.get(off + i));
		}
	}

}
