/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import static suneido.runtime.Ops.typeName;
import static suneido.util.ByteBuffers.bufferToString;
import static suneido.util.ByteBuffers.putStringToByteBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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
	/** for Dnum */
	public static final class Tag2 {
		public static final byte FALSE = 0;
		public static final byte TRUE = 1;
		public static final byte NEG_INF = 2;
		public static final byte MINUS = 3;
		public static final byte ZERO = 4;
		public static final byte PLUS = 5;
		public static final byte POS_INF = 6;
		public static final byte STRING = 7;
		public static final byte DATE = 8;
		public static final byte OBJECT = 9;
		public static final byte RECORD = 10;
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
		ByteBuffer buf = ByteBuffer.allocate(packSizeLong(n));
		packLong(n, buf);
		buf.rewind();
		return buf;
	}

	public static int packSize(Object x) {
		Class<?> xType = x.getClass();
		if (xType == Boolean.class)
			return 1;
		if (xType == Integer.class)
			return packSizeLong((Integer) x);
		if (xType == Long.class)
			return packSizeLong((Long) x);
		if (xType == Dnum.class)
			return packSizeDnum((Dnum) x);
		if (xType == String.class)
			return packSize((String) x);
		if (x instanceof Packable)
			return ((Packable) x).packSize(0);
		throw new SuException("can't pack " + typeName(x));
	}

	public static int packSize(String s) {
		// TODO: Should strings be packed in the future as UTF-16?
		int n = s.length();
		return n == 0 ? 0 : 1 + n;
	}

	public static void pack(Object x, ByteBuffer buf) {
		assert buf.order() == ByteOrder.BIG_ENDIAN;
		Class<?> xType = x.getClass();
		if (xType == Boolean.class)
			buf.put(x == Boolean.TRUE ? Tag.TRUE : Tag.FALSE);
		else if (xType == Integer.class)
			packLong((int) x, buf);
		else if (xType == Long.class)
			packLong((long) x, buf);
		else if (xType == Dnum.class)
			packDnum((Dnum) x, buf);
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

	// numbers =================================================================
	// NOTE: this is the old pack format, see PackDnum for the new format

	static int pow10[] = { 1, 10, 100, 1000 };

	/** limited by 16 digit pack format */
	public static int packSizeLong(long n) {
		if (n == 0)
			return 1;
		if (n < 0)
			n = -n;
		while (n % 10000 == 0)
			n /= 10000;
		if (n < 10000)
			return 4;
		else if (n < 1_0000_0000)
			return 6;
		else if (n < 1_0000_0000_0000L)
			return 8;
		else
			return 10;
	}

	private static int packSizeDnum(Dnum dn) {
		if (dn.isZero())
			return 1;
		if (dn.isInf())
			return 2;
		int e = dn.exp();
		long n = dn.coef();
		int p = e > 0 ? 4 - (e % 4) : Math.abs(e) % 4;
		if (p != 4)
			n /= pow10[p];
		n %= 1_0000_0000_0000L;
		if (n == 0)
			return 4;
		n %= 1_0000_0000;
		if (n == 0)
			return 6;
		n %= 1_0000;
		if (n == 0)
			return 8;
		return 10;
	}

	/** limited by 16 digit pack format */
	public static void packLong(long n, ByteBuffer buf) {
		boolean minus = n < 0;
		buf.put(minus ? Tag.MINUS : Tag.PLUS);
		if (n == 0)
			return;
		if (minus)
			n = -n;
		int e = 0;
		while (n % 10000 == 0) {
			n /= 10000;
			++e;
		}
		short sh[] = new short[4];
		int i;
		for (i = 0; n != 0; ++i) {
			long n1 = n % 10000;
			sh[i] = (short) (minus ? ~n1 : n1);
			n /= 10000;
		}
		e = e + i ^ 0x80;
		buf.put((byte) (minus ? ~e : e));
		while (--i >= 0)
			buf.putShort(sh[i]);
	}

	private static void packDnum(Dnum dn, ByteBuffer buf) {
		int sign = dn.signum();
		buf.put(sign < 0 ? Tag.MINUS : Tag.PLUS);
		if (dn.isInf())
			buf.put(sign < 0 ? 0 : (byte) 255);
		else if (sign != 0)
			packNum(sign < 0, dn.coef(), dn.exp(), buf);
	}

	private static void packNum(boolean minus, long n, int e, ByteBuffer buf) {
		int p = e > 0 ? 4 - (e % 4) : Math.abs(e) % 4;
		if (p != 4)
			{
			n /= pow10[p]; // may lose up to 3 digits of precision
			e += p;
			}
		e /= 4;
		buf.put(scale(e, minus));
		packCoef(buf, n, minus);
	}

	private static byte scale(int e, boolean minus) {
		byte eb = (byte) ((e ^ 0x80) & 0xff);
		if (minus)
			eb = (byte) ((~eb) & 0xff);
		return eb;
	}

	/** for maximized coef */
	private static void packCoef(ByteBuffer buf, long n, boolean minus) {
		putDigit(buf, minus, (short) (n / 1_0000_0000_0000L));
		n %= 1_0000_0000_0000L;
		if (n == 0)
			return;
		putDigit(buf, minus, (short) (n / 1_0000_0000));
		n %= 1_0000_0000;
		if (n == 0)
			return;
		putDigit(buf, minus, (short) (n / 1_0000));
		n %= 1_0000;
		if (n == 0)
			return;
		putDigit(buf, minus, (short) n);
	}

	private static void putDigit(ByteBuffer buf, boolean minus, short n) {
		if (minus)
			n = (short) ~n;
		buf.putShort(n);
	}

	public static int packSize(Object x, int nest) {
		return x instanceof SuContainer ? ((SuContainer) x).packSize(nest)
				: packSize(x);
	}

	// --------------------------------------------------------------

	public static Object unpack(ByteBuffer buf) {
		if (buf.remaining() == 0)
			return "";
		assert buf.order() == ByteOrder.BIG_ENDIAN;
		switch (buf.get()) {
		case Tag.FALSE:
			return Boolean.FALSE;
		case Tag.TRUE:
			return Boolean.TRUE;
		case Tag.MINUS:
		case Tag.PLUS:
			return unpackNum(buf);
		case Tag.STRING:
			return unpackString(buf);
		case Tag.OBJECT:
			return SuContainer.unpack(buf);
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

	public static long unpackLong(ByteBuffer buf) {
		byte t = buf.get();
		if (t != Tag.MINUS && t != Tag.PLUS)
			throw new SuException("unpackInt unexpected type");
		if (buf.remaining() == 0)
			return 0;
		boolean minus = (t == Tag.MINUS);
		int e = buf.get() & 0xff;
		if (e == 0 || e == 255)
			throw new SuException("unpackInt got infinity");
		if (minus)
			e = ((~e) & 0xff);
		e = (byte) (e ^ 0x80);
		int sz = buf.remaining();
		e = (e - sz / 2);
		long n = unpackLongPart(buf, minus);
		for (; e > 0; --e)
			n *= 10000;
		return minus ? -n : n;
	}

	private static final long MAX_SHIFTABLE = Integer.MAX_VALUE / 10000;

	private static Object unpackNum(ByteBuffer buf) {
		if (buf.remaining() == 0)
			return 0;
		boolean minus = buf.get(buf.position() - 1) == Tag.MINUS;
		int e = buf.get() & 0xff;
		if (e == 0)
			return Dnum.MinusInf;
		if (e == 255)
			return Dnum.Inf;
		if (minus)
			e = ((~e) & 0xff);
		e = (byte) (e ^ 0x80);
		e = (e - buf.remaining() / 2);
		// unpack min coef for easy conversion to integer
		long n = unpackLongPart(buf, minus);
		for (; 1 <= e && e <= 2 && n <= MAX_SHIFTABLE; --e)
			n *= 10000;
		if (e == 0 && n <= Integer.MAX_VALUE)
			return (int)(minus ? -n : n);
		return Dnum.from(minus ? -1 : +1, n, 4 * e + Dnum.MAX_DIGITS);
	}

	/** unsigned, min coef */
	private static long unpackLongPart(ByteBuffer buf, boolean minus) {
		int flip = minus ? 0xffff : 0;
		long n = 0;
		while (buf.remaining() > 0)
			n = n * 10000 + (short) (buf.getShort() ^ flip);
		return n;
	}

}
