/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import java.nio.ByteBuffer;

import suneido.runtime.Pack.Tag;
import suneido.util.Dnum;

/**
 * Numeric pack format is:
 * <li>Tag.MINUS, Tag.PLUS
 * <li>zero is just Tag.PLUS
 * <li>exponent converted to compare as unsigned byte
 * <li>coefficient encoded as one byte per two decimal digits
 *
 * WARNING: this pack format is NOT compatible with cSuneido
 * See Pack for the old format
 */
public class PackDnum {
	private static final long E14 = 100_000_000_000_000L;
	private static final long E12 = 1_000_000_000_000L;
	private static final long E10 = 10_000_000_000L;
	private static final long E8 = 100_000_000L;
	private static final long E6 = 1_000_000L;
	private static final long E4 = 10_000L;
	private static final long E2 = 100;

	// pack size ----------------------------------------------------

	public static int packSize(long n) {
		return packSize(Dnum.from(n));
	}

	public static int packSize(Dnum num) {
		return packSize(num.sign(), num.coef(), num.exp());
	}

	public static int packSize(int sign, long n, int e) {
		if (sign == 0)
			return 1; // just tag
		if (sign == Dnum.NEG_INF || sign == Dnum.POS_INF)
			return 3; // just tag
		return 2 + coefBytes(n);
	}

	// package visibility for test
	static int coefBytes(long coef) {
		coef %= E14;
		if (coef == 0)
			return 1;
		coef %= E12;
		if (coef == 0)
			return 2;
		coef %= E10;
		if (coef == 0)
			return 3;
		coef %= E8;
		if (coef == 0)
			return 4;
		coef %= E6;
		if (coef == 0)
			return 5;
		coef %= E4;
		if (coef == 0)
			return 6;
		coef %= E2;
		if (coef == 0)
			return 7;
		return 8;
	}

	// pack ---------------------------------------------------------

	public static void pack(long n, ByteBuffer buf) {
		pack(Dnum.from(n), buf);
	}

	public static void pack(Dnum num, ByteBuffer buf) {
		pack(num.sign(), num.coef(), num.exp(), buf);
	}

	public static void pack(int sign, long coef, int e, ByteBuffer buf) {
		// sign
		buf.put(sign < 0 ? Tag.MINUS : Tag.PLUS);
		if (sign == 0)
			return;
		if (sign == Dnum.NEG_INF) {
			buf.putShort((short) 0x0000);
			return;
		}
		if (sign == Dnum.POS_INF) {
			buf.putShort((short) 0xffff);
			return;
		}
		int xor = (sign < 0) ? 0xff : 0;

		// exponent
		buf.put((byte) (e ^ 0x80 ^ xor));

		// coefficient
		buf.put((byte) ((coef / E14) ^ xor));
		coef %= E14;
		if (coef == 0)
			return;
		buf.put((byte) ((coef / E12) ^ xor));
		coef %= E12;
		if (coef == 0)
			return;
		buf.put((byte) ((coef / E10) ^ xor));
		coef %= E10;
		if (coef == 0)
			return;
		buf.put((byte) ((coef / E8) ^ xor));
		coef %= E8;
		if (coef == 0)
			return;
		buf.put((byte) ((coef / E6) ^ xor));
		coef %= E6;
		if (coef == 0)
			return;
		buf.put((byte) ((coef / E4) ^ xor));
		coef %= E4;
		if (coef == 0)
			return;
		buf.put((byte) ((coef / E2) ^ xor));
		coef %= E2;
		if (coef == 0)
			return;
		buf.put((byte) (coef ^ xor));
	}

	// unpack -------------------------------------------------------

	/** buf is already past tag */
	public static Object unpack(ByteBuffer buf) {
		if (buf.remaining() == 0)
			return 0;
		// sign // back up to tag
		byte sign = (byte) (buf.get(buf.position() - 1) == Tag.MINUS ? -1 : +1);
		int xor = (sign < 0) ? -1 : 0;

		// exponent
		byte exp = (byte) (buf.get() ^ xor ^ 0x80);

		int pos = buf.position();
		var b = buf.get(pos) ^ xor;
		if (b == -1) {
			return sign < 0 ? Dnum.MinusInf : Dnum.Inf;
		}
		// coefficient
		long coef = 0;
		switch (buf.remaining()) {
		case 8: coef += (byte) (xor ^ buf.get(pos + 7)); // fall through
		case 7: coef += (byte) (xor ^ buf.get(pos + 6)) * E2; // fall through
		case 6: coef += (byte) (xor ^ buf.get(pos + 5)) * E4; // fall through
		case 5: coef += (byte) (xor ^ buf.get(pos + 4)) * E6; // fall through
		case 4: coef += (byte) (xor ^ buf.get(pos + 3)) * E8; // fall through
		case 3: coef += (byte) (xor ^ buf.get(pos + 2)) * E10; // fall through
		case 2: coef += (byte) (xor ^ buf.get(pos + 1)) * E12; // fall through
		case 1: coef += (byte) (xor ^ buf.get(pos + 0)) * E14;
		}
		int x = Dnum.intOrMin(sign, coef, exp);
		if (x != Integer.MIN_VALUE)
			return x;
		return Dnum.from(sign, coef, exp);
	}

	// long versions ------------------------------------------------

	public static int packSizeLong(long n) {
		if (n == 0) {
			return 1;
		}
		byte sign = +1;
		if (n < 0) {
			n = -n;
			sign = -1;
		}
		var p = Dnum.maxShift(n);
		n *= Dnum.pow10[p];
		var exp = Dnum.MAX_DIGITS - p;
		return packSize(sign, n, exp);
	}

	public static void packLong(long n, ByteBuffer buf) {
		if (n == 0) {
			buf.put(Tag.PLUS);
			return;
		}
		byte sign = +1;
		if (n < 0) {
			n = -n;
			sign = -1;
		}
		var p = Dnum.maxShift(n);
		n *= Dnum.pow10[p];
		var exp = Dnum.MAX_DIGITS - p;
		pack(sign, n, exp, buf);
	}

	// duplication with unpack, to avoid boxing
	public static long unpackLong(ByteBuffer buf) {
		buf.get();
		if (buf.remaining() == 0)
			return 0;
		// sign // back up to tag
		byte sign = (byte) (buf.get(buf.position() - 1) == Tag.MINUS ? -1 : +1);
		int xor = (sign < 0) ? -1 : 0;

		// exponent
		byte exp = (byte) (buf.get() ^ xor ^ 0x80);

		int pos = buf.position();
		var b = buf.get(pos) ^ xor;
		assert b != -1;

		// coefficient
		long coef = 0;
		switch (buf.remaining()) {
		case 8: coef += (byte) (xor ^ buf.get(pos + 7)); // fall through
		case 7: coef += (byte) (xor ^ buf.get(pos + 6)) * E2; // fall through
		case 6: coef += (byte) (xor ^ buf.get(pos + 5)) * E4; // fall through
		case 5: coef += (byte) (xor ^ buf.get(pos + 4)) * E6; // fall through
		case 4: coef += (byte) (xor ^ buf.get(pos + 3)) * E8; // fall through
		case 3: coef += (byte) (xor ^ buf.get(pos + 2)) * E10; // fall through
		case 2: coef += (byte) (xor ^ buf.get(pos + 1)) * E12; // fall through
		case 1: coef += (byte) (xor ^ buf.get(pos + 0)) * E14;
		}
		long x = Dnum.longOrMin(sign, coef, exp);
		assert x != Long.MIN_VALUE;
		return x;
	}

}
