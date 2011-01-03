/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class MemRecord extends Record {
	private final List<Data> data;

	public MemRecord() {
		data = Lists.newArrayList();
	}

	public MemRecord(DbRecord dbr) {
		data = Lists.newArrayList(dbr);
	}

	@Override
	public boolean add(Data buf) {
		data.add(buf);
		return true;
	}

	@Override
	public Data get(int i) {
		return data.get(i);
	}

	@Override
	public int size() {
		return data.size();
	}

	public ByteBuffer asByteBuffer() {
		Info info = info();
		ByteBuffer buf = ByteBuffer.allocate(info.length);
		toByteBuffer(buf, info);
		return buf;
	}

	int persist(MmapFile mmf) {
		Info info = info();
		long offset = mmf.alloc(info.length);
		ByteBuffer buf = mmf.buffer(offset);
		toByteBuffer(buf, info);
		return IntLongs.longToInt(offset);
	}

	// format must match cSuneido
	// values must be stored little endian (least significant first)
	private void toByteBuffer(ByteBuffer buf, Info info) {
		buf.order(ByteOrder.LITTLE_ENDIAN);
		buf.put((byte) info.type);
		buf.put((byte) 0);
		assert data.size() < Short.MAX_VALUE;
		buf.putShort((short) data.size());
		switch (info.type) {
		case 'c':
			byte c_offset = (byte) info.length;
			buf.put(c_offset);
			for (Data b : data)
				buf.put(c_offset -= b.length());
			break;
		case 's':
			short s_offset = (short) info.length;
			buf.putShort(s_offset);
			for (Data b : data)
				buf.putShort(s_offset -= b.length());
			break;
		case 'l':
			short l_offset = (short) info.length;
			buf.putInt(l_offset);
			for (Data b : data)
				buf.putInt(l_offset -= b.length());
			break;
		default:
			throw new Error("bad record type: " + info.type);
		}
		for (Data b : Iterables.reverse(data))
			b.addTo(buf);
	}

	private static class Info {
		public final char type;
		public final int length;
		public Info(char type, int length) {
			this.type = type;
			this.length = length;
		}
	}

	private Info info() {
		int datasize = 0;
		for (Data b : data)
			datasize += b.length();
		return dbSize(data.size(), datasize);
	}

	private static Info dbSize(int nfields, int datasize) {
		int e = 1;
		int length = 2 /* type */+ 2 /* nfields */+ e /* size */+ nfields * e + datasize;
		if (length < 0x100)
			return new Info('c', length);
		e = 2;
		length = 2 /* type */+ 2 /* nfields */+ e /* size */+ nfields * e + datasize;
		if (length < 0x10000)
			return new Info('s', length);
		e = 4;
		length = 2 /* type */+ 2 /* nfields */+ e /* size */+ nfields * e + datasize;
		return new Info('l', length);
	}

	@Override
	public int length() {
		return info().length;
	}

	@Override
	public void addTo(ByteBuffer buf) {
		toByteBuffer(buf, info());
	}

	@Override
	public byte[] asArray() {
		throw new UnsupportedOperationException();
	}

	@Override
	public byte byteAt(int i) {
		throw new UnsupportedOperationException();
	}

}
