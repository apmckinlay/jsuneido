/* Copyright 2018 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

/**
 * Divide algorithm used by Dnum.
 * Simplified version of BigDecimal multiplyDivideAndRound, divideAndRound128
 * See also Hacker's Delight and Knuth TAoCP Vol 2
 */
public class Div128 {
	private final static long E16 = 10_000_000_000_000_000L;
	private final static long LONG_MASK = 0xffffffffL;
	private static final long DIV_NUM_BASE = (1L<<32);
	private static final boolean skipChecks = true;
	private static final long e16_hi = E16 >>> 32;
	private static final long e16_lo = E16 & LONG_MASK;

	/** @return (1e16 * dividend) / divisor */
	public static long divide(long dividend, long divisor) {
		assert dividend != 0;
		assert divisor != 0;
		// multiply dividend * E16
		long d1_hi = dividend >>> 32;
		long d1_lo = dividend & LONG_MASK;
		long product = e16_lo * d1_lo;
		long d0 = product & LONG_MASK;
		long d1 = product >>> 32;
		product = e16_hi * d1_lo + d1;
		d1 = product & LONG_MASK;
		long d2 = product >>> 32;
		product = e16_lo * d1_hi + d1;
		d1 = product & LONG_MASK;
		d2 += product >>> 32;
		long d3 = d2>>>32;
		d2 &= LONG_MASK;
		product = e16_hi*d1_hi + d2;
		d2 = product & LONG_MASK;
		d3 = ((product>>>32) + d3) & LONG_MASK;
		final long dividendHi = make64(d3,d2);
		final long dividendLo = make64(d1,d0);
		// divide
		return divide128(dividendHi, dividendLo, divisor);
	}

	@SuppressWarnings("unused") // for skipChecks
	private static long divide128(long dividendHi, long dividendLo, long divisor) {
		// so we can shift dividend as much as divisor
		// don't allow equals to avoid quotient overflow (by 1)
		assert skipChecks || dividendHi < divisor;

		// maximize divisor (bit wise), since we're mostly using the top half
		final int shift = Long.numberOfLeadingZeros(divisor);
		divisor <<= shift;

		// split divisor
		final long v1 = divisor >>> 32;
		final long v0 = divisor & LONG_MASK;

		// matching shift
		final long dls = dividendLo << shift;
		// split dividendLo
		final long u1 = dls >>> 32;
		final long u0 = dls & LONG_MASK;

		// tmp1 = top 64 of dividend << shift
		final long tmp1 = (dividendHi << shift) | (dividendLo >>> 64 - shift);
		long q1, r_tmp1;
		if (v1 == 1) {
			q1 = tmp1;
			r_tmp1 = 0;
		} else {
			assert skipChecks || tmp1 >= 0;
			q1 = tmp1 / v1; // DIVIDE top 64 / top 32
			r_tmp1 = tmp1 - q1 * v1; // remainder
		}

		// adjust if quotient estimate too large
		assert skipChecks || q1 < DIV_NUM_BASE;
		while (unsignedLongCompare(q1*v0, make64(r_tmp1,u1))) {
			// done about 5.5 per 10,000 divides
			q1--;
			r_tmp1 += v1;
			if (r_tmp1 >= DIV_NUM_BASE)
				break;
		}
		assert skipChecks || q1 >= 0;
		long u2 = tmp1 & LONG_MASK; // low half

		// u2,u1 is the MIDDLE 64 bits of the dividend ???
		final long tmp2 = mulsub(u2,u1,v1,v0,q1); // ???
		long q0, r_tmp2;
		if (v1 == 1) {
			q0 = tmp2;
			r_tmp2 = 0;
		} else if (tmp2 >= 0) {
			q0 = tmp2 / v1; // DIVIDE dividend remainder 64 / divisor high 32
			r_tmp2 = tmp2 - q0 * v1;
		} else {
			long[] rq = divRemNegativeLong(tmp2, v1);
			q0 = rq[1];
			r_tmp2 = rq[0];
		}

		// adjust if quotient estimate too large
		assert skipChecks || q0 < DIV_NUM_BASE;
		while (unsignedLongCompare(q0*v0, make64(r_tmp2,u0))) {
			// done about .33 times per divide
			q0--;
			r_tmp2 += v1;
			if (r_tmp2 >= DIV_NUM_BASE)
				break;
		}

		return make64(q1,q0);
	}

	private static long make64(long hi, long lo) {
		return hi<<32 | lo;
	}

	/** @return u1,u0 - v1,v0 * q0 */
	private static long mulsub(long u1, long u0, final long v1, final long v0, long q0) {
		long tmp = u0 - q0*v0;
		return make64(u1 + (tmp>>>32) - q0*v1, tmp & LONG_MASK);
	}

	/** @return true if one > two */
	private static boolean unsignedLongCompare(long one, long two) {
		return (one+Long.MIN_VALUE) > (two+Long.MIN_VALUE);
	}

	@SuppressWarnings("unused") // for skipChecks
	private static long[] divRemNegativeLong(long n, long d) {
		assert skipChecks || n < 0 : "Non-negative numerator " + n;
		assert skipChecks || d != 1 : "Unity denominator";

		// Approximate the quotient and remainder
		long q = (n >>> 1) / (d >>> 1);
		long r = n - q * d;

		// Correct the approximation
		while (r < 0) {
			r += d;
			q--;
		}
		while (r >= d) {
			r -= d;
			q++;
		}

		// n - q*d == r && 0 <= r < d, hence we're done.
		return new long[] {r, q};
	}
}
