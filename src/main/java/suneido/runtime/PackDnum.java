/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import static suneido.SuInternalError.unreachable;

import java.nio.ByteBuffer;

import suneido.runtime.Pack.Tag2;
import suneido.util.Dnum;

/**
 * Numeric pack format is:
 * <li>Tag2.NEG_INF, Tag2.MINUS, Tag2.ZERO, Tag2.PLUS, or Tag2.POS_INF
 * <li>only tag for Tag2.NEG_INF, Tag2.ZERO, or Tag2.POS_INF
 * <li>exponent converted to compare as unsigned byte
 * <li>coefficient encoded as one byte per two decimal digits
 *
 * WARNING: this pack format is NOT compatible with cSuneido
 * See Pack for the old format
 */
public class PackDnum {

	// pack size

	public static int packSize(long n) {
		return packSize(Dnum.from(n));
	}

	public static int packSize(Dnum num) {
		return packSize(num.sign(), num.coef(), num.exp());
	}

	public static int packSize(int sign, long n, int e) {
		if (sign == Dnum.NEG_INF || sign == 0 || sign == Dnum.POS_INF)
			return 1; // just tag
		return 2 + coefBytes(n);
	}

	// pack --------------------------------------------------------------------

	public static void pack(long n, ByteBuffer buf) {
		pack(Dnum.from(n), buf);
	}

	public static void pack(Dnum num, ByteBuffer buf) {
		pack(num.sign(), num.coef(), num.exp(), buf);
	}

	public static void pack(int sign, long n, int e, ByteBuffer buf) {
		// sign
		buf.put((byte) (Tag2.ZERO + sign));
		if (sign == Dnum.NEG_INF || sign == 0 || sign == Dnum.POS_INF)
			return;

		// exponent
		buf.put(expEncode(e, sign));

		// coefficient
		byte[] bytes = new byte[8];
		int nb = coefBytes(n, bytes);
		int x = (sign < 0) ? 0xff : 0;
		for (int i = 0; i < nb; ++i)
			buf.put((byte) (bytes[i] ^ x));
	}

	private static byte expEncode(int e, int sign) {
		byte eb = (byte) (e ^ 0x80);
		if (sign == -1)
			eb = (byte) ~eb; // reverse sort order for negative
		return eb;
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
	// package visibility for test
	static int coefBytes(long coef, byte[] bytes) {
		bytes[0] = (byte) (coef / E14);
		coef %= E14;
		if (coef == 0)
			return 1;
		bytes[1] = (byte) (coef / E12);
		coef %= E12;
		if (coef == 0)
			return 2;
		bytes[2] = (byte) (coef / E10);
		coef %= E10;
		if (coef == 0)
			return 3;
		bytes[3] = (byte) (coef / E8);
		coef %= E8;
		if (coef == 0)
			return 4;
		bytes[4] = (byte) (coef / E6);
		coef %= E6;
		if (coef == 0)
			return 5;
		bytes[5] = (byte) (coef / E4);
		coef %= E4;
		if (coef == 0)
			return 6;
		bytes[6] = (byte) (coef / E2);
		coef %= E2;
		if (coef == 0)
			return 7;
		bytes[7] = (byte) coef;
		return 8;
	}
	private static final long E14 = 100_000_000_000_000L;
	private static final long E12 = 1_000_000_000_000L;
	private static final long E10 = 10_000_000_000L;
	private static final long E8 = 100_000_000L;
	private static final long E6 = 1_000_000L;
	private static final long E4 = 10_000L;
	private static final long E2 = 100;

	// unpack ------------------------------------------------------------------

	/** buf is already past tag */
	public static Dnum unpack(ByteBuffer buf) {
		// sign
		int sign = buf.get(buf.position() - 1); // back up to tag
		switch (sign) {
		case Tag2.NEG_INF :
			return Dnum.MinusInf;
		case Tag2.ZERO :
			return Dnum.Zero;
		case Tag2.POS_INF :
			return Dnum.Inf;
		case Tag2.MINUS :
			sign = -1;
			break;
		case Tag2.PLUS :
			sign = +1;
			break;
		default:
			throw unreachable();
		}
		int x = (sign < 0) ? 0xff : 0;

		// exponent
		int e = buf.get();
		e ^= x;
		e = (byte) (e ^ 0x80);

		// coefficient
		long coef = 0;
		int pos = buf.position();
		switch (buf.remaining()) {
		case 8: coef += (byte) (x ^ buf.get(pos + 7)); // fall through
		case 7: coef += (byte) (x ^ buf.get(pos + 6)) * E2; // fall through
		case 6: coef += (byte) (x ^ buf.get(pos + 5)) * E4; // fall through
		case 5: coef += (byte) (x ^ buf.get(pos + 4)) * E6; // fall through
		case 4: coef += (byte) (x ^ buf.get(pos + 3)) * E8; // fall through
		case 3: coef += (byte) (x ^ buf.get(pos + 2)) * E10; // fall through
		case 2: coef += (byte) (x ^ buf.get(pos + 1)) * E12; // fall through
		case 1: coef += (byte) (x ^ buf.get(pos + 0)) * E14;
		}

		return Dnum.from(sign, coef, e);
	}

}
