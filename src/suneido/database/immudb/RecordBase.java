/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;
import java.util.AbstractList;

import javax.annotation.concurrent.Immutable;

@Immutable
public abstract class RecordBase<T> extends AbstractList<T>
		implements Bufferable {
	protected static final ByteBuffer emptyRecBuf = emptyRec();
	public final ByteBuffer buf;
	public final int offset;

	static ByteBuffer emptyRec() {
		ByteBuffer buf = ByteBuffer.allocate(5);
		buf.put(Type.BYTE);
		buf.putInt(0);
		return buf;
	}

	protected RecordBase() {
		this(emptyRecBuf, 0);
	}

	public RecordBase(ByteBuffer buf, int offset) {
		this.buf = buf;
		this.offset = offset;
	}

	byte type() {
		return buf.get(offset + Offset.TYPE);
	}

	public int fieldLength(int i) {
		if (i >= size())
			return 0;
		return getOffset(i - 1) - getOffset(i);
	}

	@Override
	public int size() {
		int si = offset + Offset.NFIELDS;
		return (buf.get(si) & 0xff) + ((buf.get(si + 1) & 0xff) << 8);
	}

	protected int getOffset(int i) {
		// to match cSuneido use little endian (least significant first)
		switch (type()) {
		case Type.BYTE:
			return buf.get(offset + Offset.SIZE + i + 1) & 0xff;
		case Type.SHORT:
			int si = offset + Offset.SIZE + 2 * (i + 1);
			return (buf.get(si) & 0xff) + ((buf.get(si + 1) & 0xff) << 8);
		case Type.INT:
			int ii = offset + Offset.SIZE + 4 * (i + 1);
			return (buf.get(ii) & 0xff) |
					((buf.get(ii + 1) & 0xff) << 8) |
			 		((buf.get(ii + 2) & 0xff) << 16) |
			 		((buf.get(ii + 3) & 0xff) << 24);
		default:
			throw new Error("invalid record type: " + type());
		}
	}

	private static class Type {
		final static byte BYTE = 'c';
		final static byte SHORT = 's';
		final static byte INT = 'l';
	}

	private static class Offset {
		final static int TYPE = 0; // byte
		final static int NFIELDS = 2; // short
		final static int SIZE = 4; // byte, short, or int <= type
	}

	public String toDebugString() {
		String s = "";
		s += "type: " + (char) type() +
				" size: " + size() +
				" length: " + length();
//		for (int i = 0; i < Math.min(size(), 10); ++i)
//			System.out.println("offset " + i + ": " + getOffset(i));
		return s;
	}

	@Override
	public int nBufferable() {
		return 1;
	}

	@Override
	public int lengths(int[] lengths, int at) {
		lengths[at] = getOffset(-1);
		return 1;
	}

	public int length() {
		return getOffset(-1);
	}

	@Override
	public void addTo(ByteBuffer dst) {
		int n = length();
		for (int i = 0; i < n; ++i)
			dst.put(buf.get(offset + i));
	}

	public void addFieldTo(int fld, ByteBuffer dst) {
		// offset + getOffset(i), fieldLength(i)
		int from = offset + getOffset(fld);
		int lim = from + fieldLength(fld);
		for (int i = from; i < lim; ++i)
			dst.put(buf.get(i));
	}

}
