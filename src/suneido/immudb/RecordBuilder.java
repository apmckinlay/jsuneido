/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import gnu.trove.list.array.TIntArrayList;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import suneido.immudb.Record.Mode;
import suneido.language.Pack;

import com.google.common.collect.Lists;

class RecordBuilder implements suneido.intfc.database.RecordBuilder {
	private final List<ByteBuffer> bufs = Lists.newArrayList();
	private final TIntArrayList offs = new TIntArrayList();
	private final TIntArrayList lens = new TIntArrayList();

	RecordBuilder() {
	}

	/** add a field of the record */
	RecordBuilder add(Record r, int i) {
		add1(r.fieldBuffer(i), r.fieldOffset(i), r.fieldLength(i));
		return this;
	}

	/** add a prefix of the fields of the record */
	RecordBuilder addPrefix(Record rec, int prefixLength) {
		for (int i = 0; i < prefixLength; ++i)
			add1(rec.fieldBuffer(i), rec.fieldOffset(i), rec.fieldLength(i));
		return this;
	}

	RecordBuilder addFields(Record rec, int... fields) {
		for (int f : fields)
			add(rec, f);
		return this;
	}

	@Override
	public RecordBuilder add(int n) {
		ByteBuffer buf = Pack.packLong(n);
		add1(buf, 0, buf.remaining());
		return this;
	}

	/** add an unsigned int
	 * needs to be unsigned so that intrefs compare > database offsets
	 */
	public RecordBuilder adduint(int n) {
		ByteBuffer buf = Pack.packLong(n & 0xffffffffL);
		add1(buf, 0, buf.remaining());
		return this;
	}

	@Override
	public RecordBuilder add(Object x) {
		ByteBuffer buf = Pack.pack(x);
		add1(buf, 0, buf.remaining());
		return this;
	}

	private void add1(ByteBuffer buf, int off, int len) {
		bufs.add(buf);
		offs.add(off);
		lens.add(len);
	}

	@Override
	public RecordBuilder addAll(suneido.intfc.database.Record rec) {
		Record r = (Record) rec;
		for (int i = 0; i < r.size(); ++i)
			add1(r.fieldBuffer(i), r.fieldOffset(i), r.fieldLength(i));
		return this;
	}

	@Override
	public RecordBuilder add(ByteBuffer buf) {
		add1(buf, buf.position(), buf.remaining());
		return this;
	}

	@Override
	public RecordBuilder addMin() {
		return add(suneido.intfc.database.Record.MIN_FIELD);
	}

	@Override
	public RecordBuilder addMax() {
		return add(suneido.intfc.database.Record.MAX_FIELD);
	}

	@Override
	public RecordBuilder truncate(int n) {
		for (int i = bufs.size() - 1; i >= n; --i) {
			bufs.remove(i);
			offs.removeAt(i);
			lens.removeAt(i);
		}
		return this;
	}

	// build ===================================================================

	@Override
	public Record build() {
		assert bufs.size() == lens.size() && lens.size() == offs.size();
		int length = length();
		ByteBuffer buf = ByteBuffer.allocate(length());
		pack(buf, length);
		return new Record(buf, 0);
	}

	private int length() {
		int nfields = bufs.size();
		int datasize = 0;
		for (int i = 0; i < nfields; ++i)
			datasize += lens.get(i);
		return length(nfields, datasize);
	}

	static int length(int nfields, int datasize) {
		// Mode.BYTE
		int length = 2 + (1 + nfields) + datasize;
		if (length < 0x100)
			return length;
		// Mode.SHORT
		length = 2 + 2 * (1 + nfields) + datasize;
		if (length < 0x10000)
			return length;
		// Mode.INT
		return 2 + 4 * (1 + nfields) + datasize;
	}

	private void pack(ByteBuffer dst, int length) {
		packHeader(dst, length, lens);
		int nfields = bufs.size();
		for (int i = nfields - 1; i >= 0; --i)
			pack1(dst, bufs.get(i), offs.get(i), lens.get(i));
	}

	static void packHeader(ByteBuffer dst, int length, TIntArrayList lens) {
		dst.order(ByteOrder.LITTLE_ENDIAN); // to match cSuneido format
		int mode = mode(length);
		dst.putShort(header(mode, lens.size()));
		packOffsets(dst, length, lens, mode);
		dst.order(ByteOrder.BIG_ENDIAN);
	}
	private static int mode(int length) {
		if (length < 0x100)
			return Mode.BYTE;
		else if (length < 0x10000)
			return Mode.SHORT;
		else
			return Mode.INT;
	}
	private static short header(int mode, int nfields) {
		assert nfields < (1 << 14);
		return (short) ((mode << 14) | nfields);
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
