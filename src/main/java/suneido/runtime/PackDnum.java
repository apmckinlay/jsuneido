/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import java.nio.ByteBuffer;

import suneido.runtime.Pack.Tag;
import suneido.util.Dnum;

/**
 * Numeric pack format is:
 * <li>Tag.PLUS or Tag.MINUS
 * <li>packed format of zero is just Tag.PLUS
 * <li>exponent adjusted as if decimal was to left of digits,
 * and encoded to compare correctly as unsigned byte
 * <li>bytes of coefficient, most significant first
 */
public class PackDnum {

	// pack size

	public static int packSize(long n) {
		return packSize(Long.signum(n), n, 0);
	}

	public static int packSize(Dnum num) {
		return packSize(num.signum(), num.coef(), num.exp());
	}

	public static int packSize(int sign, long n, int e) {
		if (n == 0)
			return 1;
		if (e == Dnum.expInf)
			return 2;
		// strip trailing zeroes to make coefficient smaller
		if (Long.remainderUnsigned(n, 10) == 0) {
			n = Long.divideUnsigned(n, 10);
			e++;
			while (n % 10 == 0) {
				n /= 10;
				e++;
			}
		}
		return 2 + nbytes(n);
	}

	// pack --------------------------------------------------------------------

	public static void pack(long n, ByteBuffer buf) {
		pack(Long.signum(n), n, 0, buf);
	}

	public static void pack(Dnum num, ByteBuffer buf) {
		pack(num.signum(), num.coef(), num.exp(), buf);
	}

	public static void pack(int sign, long n, int e, ByteBuffer buf) {
		assert sign != 0 || n == 0;
		// sign
		buf.put(sign == -1 ? Tag.MINUS : Tag.PLUS);
		if (n == 0)
			return;
		if (e == Dnum.expInf) {
			buf.put(expEncode(e, sign));
			return;
		}

		// strip trailing zeroes to make coefficient smaller
		if (Long.remainderUnsigned(n, 10) == 0) {
			n = Long.divideUnsigned(n, 10);
			e++;
			while (n % 10 == 0) {
				n /= 10;
				e++;
			}
		}
		// adjust exponent as if decimal was at start of digits (i.e. 0 <= coef < 1)
		e += ndigits(n);
		assert e <= Byte.MAX_VALUE : e;

		// exponent
		buf.put(expEncode(e, sign));

		// coefficient
		int nb = nbytes(n) - 1;
		for (int shift = 8 * nb; shift >= 0; shift -= 8) {
			int b = (int) (n >> shift);
			if (sign == -1)
				b ^= 0xff;
			buf.put((byte) b);
		}
	}

	/** @return the number of decimal digits in x */
	private static int ndigits(long x) {
		assert x != 0;
		x = Long.divideUnsigned(x, 10);
		int n = 1;
		while (x != 0) {
			x /= 10; // only need divideUnsigned first time
			n++;
		}
		return n;
	}

	private static byte expEncode(int e, int sign) {
		byte eb = (byte) ((e ^ 0x80) & 0xff);
		if (sign == -1)
			eb = (byte) ((~eb) & 0xff); // reverse sort order for negative
		return eb;
	}

	/** @return The number of bytes required to hold n */
	static int nbytes(long x) {
		int n = 0;
		for (; x != 0; ++n)
			x >>>= 8;
		return n;
	}

	// unpack ------------------------------------------------------------------

	private static final long MAX_INT_DIV_10 = Integer.MAX_VALUE / 10;

	/** buf is already past tag
	 * @return an Integer or a Dnum
	 */
	public static Object unpack(ByteBuffer buf) {
		if (buf.remaining() == 0)
			return 0;
		// sign
		int sign = buf.get(buf.position() - 1) == Tag.MINUS ? -1 : +1;
		// exponent
		int e = buf.get() & 0xff;
		if (e == 0)
			return Dnum.MinusInf;
		if (e == 255)
			return Dnum.Inf;
		if (sign == -1)
			e = ((~e) & 0xff);
		e = (byte) (e ^ 0x80);
		// coefficient
		long n = 0;
		while (buf.remaining() > 0) {
			int b = buf.get() & 0xff;
			if (sign == -1)
				b ^= 0xff;
			n = (n << 8) | b;
		}
		assert n != 0;
		e -= ndigits(n); // restore exponent to right of digits

		// return as Integer if within range
		if (0 <= e && e < 10 && Long.compareUnsigned(n, MAX_INT_DIV_10) < 0) {
			for (; e > 0 && n < MAX_INT_DIV_10; --e)
				n *= 10;
			if (e == 0 && n < Integer.MAX_VALUE)
				return sign * (int) n;
		}
		return new Dnum(sign, n, e);
	}

}
