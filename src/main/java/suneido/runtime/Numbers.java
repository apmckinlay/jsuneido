/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

import suneido.SuException;
import suneido.SuInternalError;
import suneido.runtime.builtin.NumberMethods;

/**
 * static helper methods for working with numbers.<p>
 * Used by {@link Ops} and {@link NumberMethods}
 *
 * <p>
 * TODO: There are some problems with how numbers are conceived in JSuneido.
 *       Although *literals* are reduced to 16 decimal digits of precision
 *       (using {@link #stringToNumber(String)}, there are various ways to get
 *       around this limitation using arithmetic operations. Furthermore, it
 *       isn't clear how many different types (Byte, Short, Int, Long,
 *       BigInteger, BigDecimal) either can <u>or <em>should</em></u> be
 *       floating around in the system. It appears that there are many useless
 *       branches (<em>eg</em> testing for Short). And indeed, it would be
 *       desirable to eliminate as many unnecessary number types as possible for
 *       the sake of both performance and simplicity. --VCS 20130716
 * </p>
 */
public class Numbers {
	public static final int PRECISION = 16; // to match cSuneido
	public static final MathContext MC = new MathContext(PRECISION);

	public static final double DBL_INT_MIN = Integer.MIN_VALUE;
	public static final double DBL_INT_MAX = Integer.MAX_VALUE;
	public static final double DBL_LONG_MIN = Long.MIN_VALUE;
	public static final double DBL_LONG_MAX = Long.MAX_VALUE;
	public static final BigDecimal BD_INT_MIN = BigDecimal.valueOf(Integer.MIN_VALUE);
	public static final BigDecimal BD_INT_MAX = BigDecimal.valueOf(Integer.MAX_VALUE);
	public static final BigInteger BI_INT_MIN = BigInteger.valueOf(Integer.MIN_VALUE);
	public static final BigInteger BI_INT_MAX = BigInteger.valueOf(Integer.MAX_VALUE);
	public static final BigDecimal BD_LONG_MIN = BigDecimal.valueOf(Long.MIN_VALUE);
	public static final BigDecimal BD_LONG_MAX = BigDecimal.valueOf(Long.MAX_VALUE);
	public static final BigInteger BI_LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);
	public static final BigInteger BI_LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

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

	public static BigDecimal toBigDecimal(float x) {
		return new BigDecimal(x, MC);
	}

	public static BigDecimal toBigDecimal(double x) {
		return new BigDecimal(x, MC);
	}

	public static BigDecimal toBigDecimal(Object n) {
		if (n instanceof BigDecimal)
			return ((BigDecimal) n).round(MC);
		if (longable(n))
			return new BigDecimal(((Number) n).longValue(), MC);
		if (n instanceof Float || n instanceof Double)
			return new BigDecimal(((Number) n).doubleValue(), MC);
		if (n instanceof BigInteger)
			return new BigDecimal((BigInteger) n, MC);
		throw SuInternalError.unreachable();
	}

	/** @return true if ((Number) n).longValue() is safe (returns exact value) */
	public static boolean longable(Object n) {
		return n instanceof Integer || n instanceof Long ||
				n instanceof Short || n instanceof Byte;
		//COULD include BigInteger, BigDecimal, Float, Double if integral and within range
	}

	/** @return true if n has no fractional part */
	public static boolean integral(Object n) {
		return longable(n) ||
				n instanceof BigInteger ||
				(n instanceof BigDecimal && integral((BigDecimal)n)) ||
				((n instanceof Float || n instanceof Double) &&
						Math.abs(((Number) n).doubleValue()) % 1 == 0);
	}

	public static boolean integral(BigDecimal n) {
		return n.scale() <= 0 || n.stripTrailingZeros().scale() <= 0;
	}

	public static boolean isZero(Object x) {
		return x instanceof Number ? signum((Number) x) == 0 : false;
	}

	public static boolean isZero(Number n) {
		return signum(n) == 0;
	}

	public static int signum(Number n) {
		return longable(n) ? Long.signum(n.longValue())
				: n instanceof BigDecimal ? ((BigDecimal) n).signum()
				: n instanceof BigInteger ? ((BigInteger) n).signum()
				: (int) Math.signum(n.doubleValue());
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

	/** Convert double to int or long if possible */
	public static Number narrow(double x) {
		if (Math.abs(x) % 1 == 0)
			if (DBL_INT_MIN <= x && x <= DBL_INT_MAX)
				return (int) x;
			else if (DBL_LONG_MIN <= x && x <= DBL_LONG_MAX)
				return (long) x;
		return x;
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
	 * - if float or double, return narrow(double op)
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

		if (xn instanceof Float || xn instanceof Double ||
				yn instanceof Float || yn instanceof Double)
			return narrow(xn.doubleValue() + yn.doubleValue());

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

		if (xn instanceof Float || xn instanceof Double ||
				yn instanceof Float || yn instanceof Double)
			return narrow(xn.doubleValue() - yn.doubleValue());

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

		if (xn instanceof Float || xn instanceof Double ||
				yn instanceof Float || yn instanceof Double)
			return narrow(xn.doubleValue() * yn.doubleValue());

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

		if (xn instanceof Float || xn instanceof Double ||
				yn instanceof Float || yn instanceof Double)
			return narrow(xn.doubleValue() / yn.doubleValue());

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
		if (x instanceof CharSequence)
			return stringToPlainNumber(x.toString());
		if (x instanceof Boolean)
			return (Boolean) x ? 1 : 0;
		throw new SuException("can't convert " + Ops.typeName(x) + " to number");
	}

	/**
	 * Handles hex (0x...) and octal (0...) in addition to integers and BigDecimal
	 * @return The string converted to a Number
	 */
	public static Number stringToNumber(String s) {
		try {
			if (s.startsWith("0x"))
				return (int) Long.parseLong(s.substring(2), 16);
			if (s.startsWith("0") && s.indexOf('.') == -1)
				return (int) Long.parseLong(s, 8);
		} catch (NumberFormatException e) {
			throw new SuException("can't convert to number: " + s);
		}
		return Numbers.stringToPlainNumber(s);
	}

	private static Number stringToPlainNumber(String s) {
		if (s.length() == 0)
			return 0;
		try {
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

	static int toIntFromBI(BigInteger n) {
		if (n.compareTo(Numbers.BI_INT_MIN) == -1)
			return Integer.MIN_VALUE;
		if (n.compareTo(Numbers.BI_INT_MAX) == 1)
			return Integer.MAX_VALUE;
		return n.intValue();
	}

	static int toIntFromString(String s) {
		// TODO use Integer.decode (handles hex and octal)
		if (s.equals(""))
			return 0;
		String t = s;
		int radix = 10;
		if (s.startsWith("0x") || s.startsWith("0X")) {
			radix = 16;
			t = s.substring(2);
		} else if (s.startsWith("0"))
			radix = 8;
		try {
			return Integer.parseInt(t, radix);
		} catch (NumberFormatException e) {
			throw new SuException("can't convert string to integer: " + s);
		}
	}
}
