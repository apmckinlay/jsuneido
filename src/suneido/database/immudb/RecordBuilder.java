/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import suneido.database.immudb.Record.Mode;
import suneido.language.Pack;
import suneido.util.IntArrayList;

import com.google.common.collect.Lists;

public class RecordBuilder {
	private final List<ByteBuffer> bufs = Lists.newArrayList();
	private final IntArrayList offs = new IntArrayList();
	private final IntArrayList lens = new IntArrayList();

	public RecordBuilder() {
	}

	/** add a field of the record */
	public RecordBuilder add(Record r, int i) {
		add1(r.fieldBuffer(i), r.fieldOffset(i), r.fieldLength(i));
		return this;
	}

	/** add a prefix of the fields of the record */
	public RecordBuilder addPrefix(Record rec, int prefixLength) {
		for (int i = 0; i < prefixLength; ++i)
			add1(rec.fieldBuffer(i), rec.fieldOffset(i), rec.fieldLength(i));
		return this;
	}

	/** add an unsigned int
	 * needs to be unsigned so that intrefs compare > database offsets
	 */
	public RecordBuilder add(int n) {
		ByteBuffer buf = Pack.pack(n & 0xffffffffL);
		add1(buf, 0, buf.remaining());
		return this;
	}

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

	public Record build() {
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

	public static int length(int nfields, int datasize) {
		int e = 1;
		// Mode.BYTE
		int length = 2 /* mode */+ 2 /* nfields */+ e /* size */+ nfields * e + datasize;
		if (length < 0x100)
			return length;
		e = 2;
		// Mode.SHORT
		length = 2 /* mode */+ 2 /* nfields */+ e /* size */+ nfields * e + datasize;
		if (length < 0x10000)
			return length;
		e = 4;
		// Mode.INT
		length = 2 /* mode */+ 2 /* nfields */+ e /* size */+ nfields * e + datasize;
		return length;
	}

	private void pack(ByteBuffer dst, int length) {
		int nfields = packHeader(dst, length, lens);
		for (int i = nfields - 1; i >= 0; --i)
			pack1(dst, bufs.get(i), offs.get(i), lens.get(i));
	}

	public static int packHeader(ByteBuffer dst, int length, IntArrayList lens) {
		// match cSuneido format
		dst.order(ByteOrder.LITTLE_ENDIAN);
		char mode = mode(length);
		dst.putShort((short) mode);
		int nfields = lens.size();
		assert nfields < Short.MAX_VALUE;
		dst.putShort((short) nfields);
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
		dst.order(ByteOrder.BIG_ENDIAN);
		return nfields;
	}

	private static char mode(int length) {
		if (length < 0x100)
			return Mode.BYTE;
		else if (length < 0x10000)
			return Mode.SHORT;
		else
			return Mode.INT;
	}

	private void pack1(ByteBuffer dst, ByteBuffer buf, int off, int len) {
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
