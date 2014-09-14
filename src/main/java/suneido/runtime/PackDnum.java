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
 * <li>exponent encoded to compare correctly as unsigned byte
 * <li>bytes of coefficient, most significant first
 * <li>number of bytes - 1 is in highest (first) 3 bits<br />
 * e.g. coefficient of 15 (0xf) would be encoded as 0xf (#bytes - 1 = 0)<br />
 * 		255 (0xff) would be encoded as 0x40ff
 * <li>trailing zero (decimal) digits are stripped
 * to reduce size and make comparisons work properly
 */
public class PackDnum {

	// pack --------------------------------------------------------------------

	public static void pack(long n, ByteBuffer buf) {
		int sign = n == 0 ? 0 : +1;
		if (n < 0) {
			sign = -1;
			n = -n;
		}
		pack(sign, n, 0, buf);
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
		// strip trailing zeroes
		while (Long.remainderUnsigned(n, 10) == 0) {
			n = Long.divideUnsigned(n, 10);
			e++;
		}
		// exponent
		buf.put(expEncode(e, sign));
		// coefficient
		int nb = nbytes(n) - 1;
		int shift = 8 * nb;
		buf.put((byte) ((nb << 5) | (n >> shift)));
		for (shift -= 8; shift >= 0; shift -= 8) {
			buf.put((byte) (n >> shift));
		}
	}

	private static byte expEncode(int e, int sign) {
		byte eb = (byte) ((e ^ 0x80) & 0xff);
		if (sign == -1)
			eb = (byte) ((~eb) & 0xff);
		return eb;
	}

	/**
	 * @return The number of bytes required to hold n
	 * allowing 3 bits for this count
	 */
	static int nbytes(long x) {
		int n = 1;
		for (; (x & ~0x1f) != 0; ++n)
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
		long n = buf.get() & 0x1f; // strip nbytes (only needed for sorting)
		while (buf.remaining() > 0)
			n = (n << 8) | (buf.get() & 0xff);
		assert n != 0;
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
