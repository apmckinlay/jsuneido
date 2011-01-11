/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import suneido.language.Pack;
import suneido.util.IntArrayList;

import com.google.common.collect.Lists;

public class RecordBuilder {
	private final List<Object> data = Lists.newArrayList();
	private final IntArrayList offs = new IntArrayList();
	private final IntArrayList lens = new IntArrayList();
	private int length;
	private char type;

	public RecordBuilder() {
	}

	/** adds each of the fields of the record, NOT a nested record */
	public RecordBuilder add(Record r) {
		for (int i = 0; i < r.size(); ++i)
			add1(r.buf, r.fieldOffset(i), r.fieldLength(i));
		return this;
	}

	/** adds a prefix of the fields of the record at buf,offset */
	public RecordBuilder add(ByteBuffer buf, int offset, int n) {
		for (int i = 0; i < n; ++i)
			add1(buf, Record.fieldOffset(buf, offset, i), Record.fieldLength(buf, offset, i));
		return this;
	}

	public RecordBuilder add(int n) {
		add1(null, n, Pack.packSize(n));
		return this;
	}

	public RecordBuilder add(String s) {
		add1(s, 0, Pack.packSize(s));
		return this;
	}

	private void add1(Object buf, int off, int len) {
		data.add(buf);
		offs.add(off);
		lens.add(len);
	}

	public Record build() {
		return new Record(asByteBuffer(), 0);
	}

	public ByteBuffer asByteBuffer() {
		info();
		ByteBuffer buf = ByteBuffer.allocate(length);
		toByteBuffer(buf);
		return buf;
	}

	// format must match cSuneido
	// offsets must be stored little endian (least significant first)
	private void toByteBuffer(ByteBuffer buf) {
		// to match cSuneido use little endian (least significant first)
		buf.order(ByteOrder.LITTLE_ENDIAN);
		buf.putShort((short) type);
		int nfields = data.size();
		assert nfields < Short.MAX_VALUE;
		buf.putShort((short) nfields);
		int offset = length;
		switch (type) {
		case 'c':
			buf.put((byte) offset);
			for (int i = 0; i < nfields; ++i)
				buf.put((byte) (offset -= lens.get(i)));
			break;
		case 's':
			buf.putShort((short) offset);
			for (int i = 0; i < nfields; ++i)
				buf.putShort((short) (offset -= lens.get(i)));
			break;
		case 'l':
			buf.putInt(offset);
			for (int i = 0; i < nfields; ++i)
				buf.putInt(offset -= lens.get(i));
			break;
		default:
			throw new Error("bad record type: " + type);
		}
		buf.order(ByteOrder.BIG_ENDIAN);
		for (int i = nfields - 1; i >= 0; --i)
			putItem(i, buf);
	}

	private void info() {
		int nfields = data.size();
		int datasize = 0;
		for (int i = 0; i < nfields; ++i)
			datasize += lens.get(i);
		int e = 1;
		type = 'c';
		length = 2 /* type */+ 2 /* nfields */+ e /* size */+ nfields * e + datasize;
		if (length < 0x100)
			return;
		e = 2;
		type = 's';
		length = 2 /* type */+ 2 /* nfields */+ e /* size */+ nfields * e + datasize;
		if (length < 0x10000)
			return;
		e = 4;
		type = 'l';
		length = 2 /* type */+ 2 /* nfields */+ e /* size */+ nfields * e + datasize;
	}

	private void putItem(int i, ByteBuffer dst) {
		Object x = data.get(i);
		if (x == null) // integer
			Pack.pack(offs.get(i), dst);
		else if (x instanceof ByteBuffer)
			copy((ByteBuffer) x, offs.get(i), lens.get(i), dst);
		else if (x instanceof String)
			Pack.pack(x, dst);
		else
			throw new Error("invalid data type");
	}

	private void copy(ByteBuffer buf, int off, int len, ByteBuffer dst) {
		if (buf.hasArray()) {
			byte[] a = buf.array();
			off += buf.arrayOffset();
			dst.put(a, off, len);
		} else {
			for (int i = off; i < off + len; ++i)
				dst.put(buf.get(i));
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		return sb.toString();
	}

}
