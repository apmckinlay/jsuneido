/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.annotation.concurrent.Immutable;

import suneido.SuException;

/**
 * A Record stored in a ByteBuffer.
 * Avoids slicing or duplicating the ByteBuffer
 * Does not use or modify the ByteBuffer mutable data (position, etc.)
 */
@Immutable
public class DbRecord extends Record {
	private final ByteBuffer buf;
	private Rep rep;

	public DbRecord(ByteBuffer buf) {
		this.buf = buf;
		assert buf.order() == ByteOrder.LITTLE_ENDIAN;
		switch (type()) {
		case Type.BYTE:
			rep = new ByteRep();
			break;
		case Type.SHORT:
			rep = new ShortRep();
			break;
		case Type.INT:
			rep = new IntRep();
			break;
		default:
			throw new SuException("bad record type: " + type());
		}
	}

	private byte type() {
		return buf.get(Offset.TYPE);
	}

	@Override
	public void add(Data buf) {
		throw new UnsupportedOperationException("DbRecord.add");
	}

	@Override
	public Data get(int i) {
		if (i >= size())
			return Data.EMPTY;
		return new DataBuf(buf, rep.getOffset(i), fieldSize(i));
	}

	private int fieldSize(int i) {
		if (i >= size())
			return 0;
		return rep.getOffset(i - 1) - rep.getOffset(i);
	}

	@Override
	public int size() {
		return buf.getShort(Offset.NFIELDS);
	}

	// "strategy" object to avoid switching on type.
	private abstract class Rep {
		abstract int getOffset(int i);
	}

	private class ByteRep extends Rep {
		@Override
		int getOffset(int i) {
			return buf.get(Offset.SIZE + i + 1) & 0xff;
		}
	}

	private class ShortRep extends Rep {
		@Override
		int getOffset(int i) {
			return buf.getShort(Offset.SIZE + 2 * (i + 1)) & 0xffff;
		}
	}

	private class IntRep extends Rep {
		@Override
		int getOffset(int i) {
			return buf.getInt(Offset.SIZE + 4 * (i + 1));
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
		System.out.println("length: " + rep.getOffset(-1));
		for (int i = 0; i < size(); ++i)
			System.out.println("offset " + i + ": " + rep.getOffset(i));
	}

}
