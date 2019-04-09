/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import static suneido.runtime.Ops.typeName;
import static suneido.util.ByteBuffers.bufferToString;
import static suneido.util.ByteBuffers.putStringToByteBuffer;

import java.nio.ByteBuffer;

import suneido.*;
import suneido.util.Dnum;
import suneido.util.ThreadSafe;

@ThreadSafe // all static methods
public class Pack {
	// sequence must match Order, values must match cSuneido
	public static final class Tag {
		public static final byte FALSE = 0;
		public static final byte TRUE = 1;
		public static final byte MINUS = 2;
		public static final byte PLUS = 3;
		public static final byte STRING = 4;
		public static final byte DATE = 5;
		public static final byte OBJECT = 6;
		public static final byte RECORD = 7;
	}
	private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);

	public static ByteBuffer pack(Object x) {
		int n = packSize(x);
		if (n == 0)
			return EMPTY_BUFFER;
		ByteBuffer buf = ByteBuffer.allocate(n);
		pack(x, buf);
		buf.rewind();
		return buf;
	}

	public static ByteBuffer packLong(long n) {
		ByteBuffer buf = ByteBuffer.allocate(PackDnum.packSizeLong(n));
		PackDnum.packLong(n, buf);
		buf.rewind();
		return buf;
	}

	public static int packSize(Object x) {
		Class<?> xType = x.getClass();
		if (xType == Boolean.class)
			return 1;
		if (xType == Integer.class)
			return PackDnum.packSizeLong((int) x);
		if (xType == Long.class)
			return PackDnum.packSizeLong((long) x);
		if (xType == Dnum.class)
			return PackDnum.packSize((Dnum) x);
		if (xType == String.class)
			return packSize((String) x);
		if (x instanceof Packable)
			return ((Packable) x).packSize(0);
		throw new SuException("can't pack " + typeName(x));
	}

	public static int packSize(String s) {
		int n = s.length();
		return n == 0 ? 0 : 1 + n;
	}

	public static void pack(Object x, ByteBuffer buf) {
		Class<?> xType = x.getClass();
		if (xType == Boolean.class)
			buf.put(x == Boolean.TRUE ? Tag.TRUE : Tag.FALSE);
		else if (xType == Integer.class)
			PackDnum.packLong((int) x, buf);
		else if (xType == Long.class)
			PackDnum.packLong((long) x, buf);
		else if (xType == Dnum.class)
			PackDnum.pack((Dnum) x, buf);
		else if (xType == String.class)
			packString((String) x, buf);
		else if (x instanceof Packable)
			((Packable) x).pack(buf);
		else
			throw new SuException("can't pack " + typeName(x));
	}

	static void packString(String s, ByteBuffer buf) {
		if (s.length() == 0)
			return;
		buf.put(Tag.STRING);
		putStringToByteBuffer(s, buf);
	}

	public static int packSize(Object x, int nest) {
		return x instanceof SuObject ? ((SuObject) x).packSize(nest)
				: packSize(x);
	}

	// --------------------------------------------------------------

	public static Object unpack(ByteBuffer buf) {
		if (buf.remaining() == 0)
			return "";
		switch (buf.get()) {
		case Tag.FALSE:
			return Boolean.FALSE;
		case Tag.TRUE:
			return Boolean.TRUE;
		case Tag.MINUS:
		case Tag.PLUS:
			return PackDnum.unpack(buf);
		case Tag.STRING:
			return unpackString(buf);
		case Tag.OBJECT:
			return SuObject.unpack(buf);
		case Tag.RECORD:
			return SuRecord.unpack(buf);
		case Tag.DATE:
			return SuDate.unpack(buf);
		default:
			throw new SuException("invalid unpack type: "
					+ buf.get(buf.position() - 1));
		}
	}

	private static String unpackString(ByteBuffer buf) {
		return bufferToString(buf);
	}
}
