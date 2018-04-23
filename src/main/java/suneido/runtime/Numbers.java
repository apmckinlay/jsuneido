/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import java.math.BigDecimal;
import java.math.MathContext;

import suneido.SuException;
import suneido.SuInternalError;
import suneido.runtime.builtin.NumberMethods;

/**
 * static helper methods for working with numbers.<p>
 * Used by {@link Ops} and {@link NumberMethods}
  */
public class Numbers {
	public static final int PRECISION = 16; // to match cSuneido
	public static final MathContext MC = new MathContext(PRECISION);

	public static final BigDecimal BD_INT_MIN = BigDecimal.valueOf(Integer.MIN_VALUE);
	public static final BigDecimal BD_INT_MAX = BigDecimal.valueOf(Integer.MAX_VALUE);
	public static final BigDecimal BD_LONG_MIN = BigDecimal.valueOf(Long.MIN_VALUE);
	public static final BigDecimal BD_LONG_MAX = BigDecimal.valueOf(Long.MAX_VALUE);

	public static final BigDecimal ZERO = BigDecimal.ZERO;
	public static final BigDecimal INF =
			BigDecimal.valueOf(1, -4 * Byte.MAX_VALUE);
	public static final BigDecimal MINUS_INF =
			BigDecimal.valueOf(-1, -4 * Byte.MAX_VALUE);


	private Numbers() {
	} // all static, no instances

	public static BigDecimal toBigDecimal(int x) {
		return new BigDecimal(x, MC);
	}

	public static BigDecimal toBigDecimal(long x) {
		return new BigDecimal(x, MC);
	}

	public static BigDecimal toBigDecimal(Object n) {
		if (n instanceof BigDecimal)
			return ((BigDecimal) n).round(MC);
		if (longable(n))
			return new BigDecimal(((Number) n).longValue(), MC);
		throw SuInternalError.unreachable();
	}

	/** @return true if ((Number) n).longValue() is safe (returns exact value) */
	public static boolean longable(Object n) {
		return n instanceof Integer || n instanceof Long;
	}

	/** @return true if n has no fractional part */
	public static boolean integral(Object n) {
		return longable(n) ||
				(n instanceof BigDecimal && integral((BigDecimal)n));
	}

	public static boolean integral(BigDecimal n) {
		return n.scale() <= 0 || n.stripTrailingZeros().scale() <= 0;
	}

	public static boolean isZero(Object x) {
		return x instanceof Number && signum((Number) x) == 0;
	}

	public static boolean isZero(Number n) {
		return signum(n) == 0;
	}

	public static int signum(Number n) {
		if (longable(n))
			return Long.signum(n.longValue());
		if (n instanceof BigDecimal)
			return ((BigDecimal) n).signum();
		throw new SuException("signum unsupported type");
	}

	public static boolean isInRange(BigDecimal x, BigDecimal lo, BigDecimal hi) {
		return x.compareTo(lo) >= 0 && x.compareTo(hi) <= 0;
	}

	public static Number narrow(long x) {
		if (Integer.MIN_VALUE <= x && x <= Integer.MAX_VALUE)
			return (int) x;
		else
			return x;
		// NOTE: can't use ?: because it would convert to same type
	}

	/** Convert BigDecimal to int or long if possible */
	public static Number narrow(BigDecimal x) {
		if (x.signum() == 0)
			return 0; // TODO: Might it not be cleaner to remove this 'optimization'?
		else if (integral(x)) {
			if (isInRange(x, BD_INT_MIN, BD_INT_MAX))
				return x.intValueExact();
			else if (isInRange(x, BD_LONG_MIN, BD_LONG_MAX))
				return x.longValueExact();
		}
		return x;
	}

	/** Ensure number is in narrowest representation possible */
	public static Number narrow(Number x) {
		if (x instanceof Long) {
			return narrow(x.longValue());
		} else if (x instanceof BigDecimal) {
			return narrow((BigDecimal)x);
		}
		return x;
	}

	/*
	 * add2, sub2, mul2, and div2 follow same outline
	 * - convert to numbers (throws if not convertible)
	 * - check for zero
	 * - check for infinite
	 * - return bigdecimal op
	 */

	public static Number add2(Object x_, Object y_) {
		Number xn = toNum(x_);
		Number yn = toNum(y_);

		if (isZero(xn))
			return yn;
		if (isZero(yn))
			return xn;

		if (xn == INF)
			return yn == MINUS_INF ? 0 : INF;
		if (yn == INF)
			return xn == MINUS_INF ? 0 : INF;
		if (xn == MINUS_INF)
			return yn == INF ? 0 : MINUS_INF;
		if (yn == MINUS_INF)
			return xn == INF ? 0 : MINUS_INF;

		return narrow(toBigDecimal(xn).add(toBigDecimal(yn), MC));
	}

	public static Number sub2(Object x_, Object y_) {
		Number xn = toNum(x_);
		Number yn = toNum(y_);

		if (isZero(yn))
			return xn;

		if (xn == INF)
			return yn == INF ? 0 : INF;
		if (yn == INF)
			return xn == INF ? 0 : MINUS_INF;
		if (xn == MINUS_INF)
			return yn == MINUS_INF ? 0 : MINUS_INF;
		if (yn == MINUS_INF)
			return xn == MINUS_INF ? 0 : INF;

		return narrow(toBigDecimal(xn).subtract(toBigDecimal(yn), MC));
	}

	public static Number mul2(Object x_, Object y_) {
		Number xn = toNum(x_);
		Number yn = toNum(y_);

		if (isZero(xn) || isZero(yn))
			return 0;

		if (xn == INF)
			return (signum(yn) < 0) ? MINUS_INF : INF;
		if (yn == INF)
			return (signum(xn) < 0) ? MINUS_INF : INF;
		if (xn == MINUS_INF)
			return (signum(yn) < 0) ? INF : MINUS_INF;
		if (yn == MINUS_INF)
			return (signum(xn) < 0) ? INF : MINUS_INF;

		return narrow(toBigDecimal(xn).multiply(toBigDecimal(yn), MC));
	}

	public static Number div2(Object x_, Object y_) {
		Number xn = toNum(x_);
		Number yn = toNum(y_);

		if (isZero(xn))
			return 0;
		if (isZero(yn))
			return signum(xn) < 0 ? MINUS_INF : INF;

		if (xn == INF)
			return yn == INF ? +1 : yn == MINUS_INF ? -1
					: (signum(yn) < 0) ? MINUS_INF : INF;
		if (xn == MINUS_INF)
			return yn == INF ? -1 : yn == MINUS_INF ? +1
					: (signum(yn) < 0) ? INF : MINUS_INF;
		if (yn == INF || yn == MINUS_INF)
			return 0;

		return narrow(toBigDecimal(xn).divide(toBigDecimal(yn), MC));
	}

	/**
	 * @return The value converted to a Number.
	 * "" is converted to 0.
	 * true and false are converted to 1 and 0.
	 * Converts strings containing integers or BigDecimal.
	 * The original value is returned if instanceof Number.
	 */
	public static Number toNum(Object x) {
		if (x instanceof Number)
			return (Number) x;
		return Ops.likeZero(x);
	}

	/**
	 * Handles hex (0x...) in addition to integers and BigDecimal
	 * @return The string converted to a Number
	 */
	public static Number stringToNumber(String s) {
		try {
			if (s.startsWith("0x"))
				return (int) Long.parseLong(s.substring(2), 16);
			if (s.indexOf('.') == -1 && s.indexOf('e') == -1
					&& s.indexOf("E") == -1 && s.length() < 10)
				return Integer.parseInt(s);
			else {
				BigDecimal n = new BigDecimal(s, MC);
				if (n.compareTo(BigDecimal.ZERO) == 0)
					return 0;
				return n;
			}
		} catch (NumberFormatException e) {
			throw new SuException("can't convert to number: " + s);
		}
	}

	static int toIntFromLong(long n) {
		if (n < Integer.MIN_VALUE)
			return Integer.MIN_VALUE;
		if (n > Integer.MAX_VALUE)
			return Integer.MAX_VALUE;
		return (int) n;
	}

	static int toIntFromBD(BigDecimal n) {
		if (n.compareTo(Numbers.BD_INT_MIN) == -1)
			return Integer.MIN_VALUE;
		if (n.compareTo(Numbers.BD_INT_MAX) == 1)
			return Integer.MAX_VALUE;
		return n.intValue();
	}
}
