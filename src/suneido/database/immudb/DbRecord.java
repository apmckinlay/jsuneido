/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.annotation.concurrent.Immutable;

/**
 * A Record stored in a ByteBuffer.
 * Avoids slicing or duplicating the ByteBuffer
 * Does not use or modify the ByteBuffer mutable data (position, etc.)
 */
@Immutable
public class DbRecord extends Record {
	protected final ByteBuffer buf;
	protected final int offset;
	protected static final ByteBuffer emptyRecBuf;
	static {
		emptyRecBuf = ByteBuffer.allocate(5);
		emptyRecBuf.order(ByteOrder.LITTLE_ENDIAN);
		emptyRecBuf.put(Type.BYTE);
		emptyRecBuf.put((byte) 0);
		emptyRecBuf.putShort((short) 0);
		emptyRecBuf.put((byte) 0);
	}
	public static final Record EMPTY = new DbRecord();

	protected DbRecord() {
		this(emptyRecBuf);
	}

	public DbRecord(ByteBuffer buf) {
		this(buf, 0);
	}

	public DbRecord(ByteBuffer buf, int offset) {
		this.buf = buf;
		this.offset = offset;
		assert buf.order() == ByteOrder.LITTLE_ENDIAN;
	}

	private byte type() {
		return buf.get(offset + Offset.TYPE);
	}

	@Override
	public boolean add(Data buf) {
		throw new UnsupportedOperationException("DbRecord.add");
	}

	@Override
	public Data get(int i) {
		if (i >= size())
			return DataBytes.EMPTY;
		return new DataBuf(buf, offset + getOffset(i), fieldSize(i));
	}

	private int fieldSize(int i) {
		if (i >= size())
			return 0;
		return getOffset(i - 1) - getOffset(i);
	}

	@Override
	public int size() {
		return buf.getShort(offset + Offset.NFIELDS);
	}

	protected int getOffset(int i) {
		switch (type()) {
		case Type.BYTE:
			return buf.get(offset + Offset.SIZE + i + 1) & 0xff;
		case Type.SHORT:
			return buf.getShort(offset + Offset.SIZE + 2 * (i + 1)) & 0xffff;
		case Type.INT:
			return buf.getInt(offset + Offset.SIZE + 4 * (i + 1));
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

	public void debugPrint() {
		System.out.println("type: " + (char) type());
		System.out.println("size: " + size());
		System.out.println("length: " + length());
		for (int i = 0; i < size(); ++i)
			System.out.println("offset " + i + ": " + getOffset(i));
	}

	@Override
	public int length() {
		return getOffset(-1);
	}

	@Override
	public void addTo(ByteBuffer dst) {
		for (int i = 0; i < length(); ++i)
			dst.put(buf.get(offset + i));
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
