/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import suneido.language.Pack;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class RecordBuilder {
	private final List<Object> data = Lists.newArrayList();
	private int length = 0;
	private int lengths[] = null;
	private char type;

	public RecordBuilder() {
	}

	public RecordBuilder(Record r) {
		data.addAll(r);
	}

	public RecordBuilder add(Object x) {
		length = 0;
		data.add(x);
		return this;
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

	int persist(MmapFile mmf) {
		info();
		int adr = mmf.alloc(length);
		ByteBuffer buf = mmf.buffer(adr);
		toByteBuffer(buf);
		return adr;
	}

	// format must match cSuneido
	// values must be stored little endian (least significant first)
	private void toByteBuffer(ByteBuffer buf) {
		// to match cSuneido use little endian (least significant first)
		buf.order(ByteOrder.LITTLE_ENDIAN);
		buf.putShort((short) type);
		int nfields = lengths.length;
		assert nfields < Short.MAX_VALUE;
		buf.putShort((short) nfields);
		int offset = length;
		switch (type) {
		case 'c':
			buf.put((byte) offset);
			for (int i = 0; i < nfields; ++i)
				buf.put((byte) (offset -= lengths[i]));
			break;
		case 's':
			buf.putShort((short) offset);
			for (int i = 0; i < nfields; ++i)
				buf.putShort((short) (offset -= lengths[i]));
			break;
		case 'l':
			buf.putInt(offset);
			for (int i = 0; i < nfields; ++i)
				buf.putInt(offset -= lengths[i]);
			break;
		default:
			throw new Error("bad record type: " + type);
		}
		buf.order(ByteOrder.BIG_ENDIAN);
		for (Object x : Iterables.reverse(data))
			putItem(x, buf);
	}

	private void info() {
		if (length != 0)
			return;
		int nfields = 0;
		for (Object x : data)
			nfields += (x instanceof Bufferable)
					? ((Bufferable) x).nBufferable() : 1;
		lengths = new int[nfields];
		int i = 0;
		for (Object x : data)
			i += itemLengths(x, i);
		int datasize = 0;
		for (int len : lengths)
			datasize += len;
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

	private int itemLengths(Object x, int i) {
		if (x instanceof Bufferable)
			return ((Bufferable) x).lengths(lengths, i);
		lengths[i] = Pack.packSize(x);
		return 1;
	}

	static void putItem(Object x, ByteBuffer buf) {
		if (x instanceof Bufferable)
			((Bufferable) x).addTo(buf);
		else
			Pack.pack(x, buf);
	}

}
