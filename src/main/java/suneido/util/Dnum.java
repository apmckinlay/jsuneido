/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.math.RoundingMode;

import com.google.common.base.Strings;

/**
 * Decimal floating point number implementation
 * using a 64 bit long (treated as unsigned) for the coefficient.
 * <p>
 * Value is sign * coef * 10^exp, zeroed value is 0.
 * <p>
 * Math "sticks" at infinite when it overflows.
 * <p>
 * There is no NaN, inf / inf = 1, 0 / ... = 0, inf / ... = inf
 */
@Immutable
public class Dnum extends Number implements Comparable<Dnum> {
	private static final long serialVersionUID = 1L;
	private final long coef;
	private final byte sign;
	private final byte exp;

	final static byte POS = +1;
	final static byte ZERO = 0;
	final static byte NEG = -1;
	public final static byte POS_INF = +2;
	public final static byte NEG_INF = -2;
	private static final int EXP_MIN = Byte.MIN_VALUE;
	private static final int EXP_MAX = Byte.MAX_VALUE;
	private final static long COEF_MAX = 9_999_999_999_999_999L;
	public final static int MAX_DIGITS = 16;
	private final static int MAX_SHIFT = MAX_DIGITS - 1;

	public final static Dnum Zero = new Dnum(ZERO, 0, 0);
	public final static Dnum One = new Dnum(POS, 1_000_000_000_000_000L, 1);
	public final static Dnum MinusOne = new Dnum(NEG, 1_000_000_000_000_000L, 1);
	public final static Dnum Inf = new Dnum(POS_INF, 1, 0);
	public final static Dnum MinusInf = new Dnum(NEG_INF, 1, 0);

	private final static long pow10[] = {
			1L,
			10L,
			100L,
			1000L,
			10000L,
			100000L,
			1000000L,
			10000000L,
			100000000L,
			1000000000L,
			10000000000L,
			100000000000L,
			1000000000000L,
			10000000000000L,
			100000000000000L,
			1000000000000000L,
			10000000000000000L,
			100000000000000000L,
			1000000000000000000L
			};

	private final static long halfpow10[] = { // for rounding
			0L,
			5L,
			50L,
			500L,
			5000L,
			50000L,
			500000L,
			5000000L,
			50000000L,
			500000000L,
			5000000000L,
			50000000000L,
			500000000000L,
			5000000000000L,
			50000000000000L,
			500000000000000L,
			5000000000000000L,
			50000000000000000L,
			500000000000000000L,
			5000000000000000000L
			};

	/** raw - no normalization */
	private Dnum(byte sign, long coef, int exp) {
		this.sign = sign;
		assert coef >= 0;
		this.coef = coef;
		assert EXP_MIN <= exp && exp <= EXP_MAX;
		this.exp = (byte) exp;
	}

	public static Dnum from(long n) {
		if (n == 0)
			return Zero;
		byte sign = POS;
		if (n < 0) {
			n = -n;
			sign = NEG;
		}
		int p = maxShift(n);
		n *= pow10[p];
		return new Dnum(sign, n, MAX_DIGITS - p);
	}

	public static Dnum from(double d) { // needed for math/trig functions
		if (Double.isNaN(d))
			throw new NumberFormatException("can't convert NaN to number");
		if (Double.isInfinite(d))
			return d < 0 ? MinusInf : Inf;
		// probably not the most efficient, but simple
		return parse(Double.toString(d));
	}

	static int ilog10(long x)
		{
		// based on Hacker's Delight
		if (x == 0)
			return 0;
		int y = (19 * (63 - Long.numberOfLeadingZeros(x))) >> 6;
		if (y < 18 && x >= pow10[y + 1])
			++y;
		return y;
		}

	// the maximum we can safely shift left (*10)
	private static int maxShift(long x)
		{
		int i = ilog10(x);
		return i > MAX_SHIFT ? 0 : MAX_SHIFT - i;
		}

	/** normalizes */
	public static Dnum from(int sign, long coef, int exp) {
		if (sign == 0 || coef == 0 || exp < EXP_MIN) {
			return Zero;
		} else if (sign == POS_INF) {
			return Inf;
		} else if (sign == NEG_INF) {
			return MinusInf;
		} else {
			boolean atmax = false;
			while (coef > COEF_MAX)
				{
				coef = (coef + 5) / 10; // drop/round least significant digit
				++exp;
				atmax = true;
				}
			if (! atmax) {
				int p = maxShift(coef);
				coef *= pow10[p];
				exp -= p;
			}
			if (exp > EXP_MAX)
				return inf(sign);
			return new Dnum(sign < 0 ? NEG : POS, coef, exp);
		}
	}

	private static class Parser {
		final String s;
		int i = 0;
		int exp = 0;

		Parser(String s) {
			this.s = s;
		}

		Dnum parse() {
			byte sign = POS;
			if (match('-'))
				sign = NEG;
			else
				match('+');

			if (s.startsWith("inf", i))
				return inf(sign);

			long coef = getCoef();
			exp += getExp();
			if (i < s.length()) // didn't consume entire string
				throw new RuntimeException("invalid number");
			if (coef == 0 || exp < EXP_MIN)
				return Zero;
			else if (exp > EXP_MAX)
				return inf(sign);
			else
				return Dnum.from(sign, coef, exp);
		}

		private long getCoef() {
			boolean digits = false;
			boolean before_decimal = true;

			// skip leading zeroes, no effect on result
			while (match('0'))
				digits = true;
			if (next() == '.' && i + 1 < s.length())
				digits = false;

			long n = 0;
			int p = MAX_SHIFT;
			while (true)
				{
				if (isdigit(next())) {
					digits = true;
					// ignore extra decimal places
					// required for double conversion
					if (next() != '0' && p >= 0)
						n += (next() - '0') * pow10[p];
					--p;
					++i;
				} else if (before_decimal) {
					exp = MAX_SHIFT - p;
					if (! match('.'))
						break;
					before_decimal = false;
					if (!digits) {
						for (; match('0'); --exp)
							digits = true;
					}
				} else
					break;
				}
			if (!digits)
				throw new RuntimeException("numbers require at least one digit");
			return n;
		}

		int getExp() {
			int e = 0;
			if (match('e') || match('E'))
				{
				int esign = match('-') ? -1 : 1;
				match('+');
				for (; isdigit(next()); ++i)
					e = e * 10 + (next() - '0');
				e *= esign;
				}
			return e;
		}
		private char next() {
			return i < s.length() ? s.charAt(i) : 0;
		}
		private boolean match(char c) {
			if (next() == c) {
				i++;
				return true;
			}
			return false;
		}
		private static boolean isdigit(char c) {
			return '0' <= c && c <= '9';
		}
	}

	public static Dnum parse(String s) {
		return new Parser(s).parse();
	}

	/**
	 * Default conversion to string.
	 * Should match cSuneido.
	 */
	@Override
	public String toString() {
		final int MAX_LEADING_ZEROS = 7;
		if (isZero())
			return "0";
		StringBuilder sb = new StringBuilder(20);
		if (sign < 0)
			sb.append('-');
		if (isInf())
			return sb.append("inf").toString();

		char digits[] = new char[MAX_DIGITS];
		int nd = digits(digits);
		int e = exp - nd;
		if (-MAX_LEADING_ZEROS <= exp && exp <= 0) {
			// decimal to the left
			sb.append('.').append(Strings.repeat("0", -e - nd)).append(digits, 0, nd);
		} else if (-nd < e && e <= -1) {
			// decimal within
			int d = nd + e;
			sb.append(digits, 0, d);
			if (nd > 1)
				sb.append('.').append(digits, d, nd - d);
		} else if (0 < exp && exp <= MAX_DIGITS) {
			// decimal to the right
			sb.append(digits, 0, nd).append(Strings.repeat("0", e));
		} else {
			// use scientific notation
			sb.append(digits, 0, 1);
			if (nd > 1)
				sb.append('.').append(digits, 1, nd - 1);
			sb.append('e').append(exp - 1);
		}
		return sb.toString();
	}

	/**
	 * Puts the decimal digits of coef into the array,
	 * without trailing zeroes.
	 * digits should be MAX_DIGITS in length.
	 * @return The number of digits placed in the array (0 to MAX_DIGITS).
	 */
	public int digits(char[] digits) {
		int i = MAX_SHIFT;
		int nd = 0;
		for (long c = coef; c != 0; --i, ++nd) {
			digits[nd] = (char) ('0' + (char) (c / pow10[i]));
			c %= pow10[i];
		}
		return nd;
	}

	/** for debug/test */
	String show() {
		StringBuilder sb = new StringBuilder();
		switch (sign) {
		case NEG_INF:	return "--";
		case NEG:		sb.append("-"); break;
		case 0:			sb.append("z"); break;
		case POS:		sb.append("+"); break;
		case POS_INF:	return "++";
		default:		sb.append("?"); break;
		}
		if (coef == 0)
			sb.append('0');
		else
			{
			sb.append(".");
			long c = coef;
			for (int i = MAX_SHIFT; i >= 0 && c != 0; --i)
				{
				long p = pow10[i];
				int digit = (int)(c / p);
				c %= p;
				sb.append((char) ('0' + digit));
				}
			}
		sb.append('e').append(exp);
		return sb.toString();
	}

	public boolean isZero() {
		return sign == ZERO;
	}

	/** @return true if plus or minus infinite */
	public boolean isInf() {
		return sign == NEG_INF || sign == POS_INF;
	}

	public Dnum negate() {
		return from(sign * -1, coef, exp);
	}

	public Dnum abs() {
		return sign < 0 ? from(-sign, coef, exp) : this;
	}

	public int signum() {
		return sign < 0 ? -1 : sign > 0 ? +1 : 0;
	}

	public int sign() {
		return sign;
	}

	public int exp() {
		return exp;
	}

	public long coef() {
		return coef;
	}

	// add and subtract --------------------------------------------------------

	public static Dnum add(Dnum x, Dnum y) {
		if (x.sign == 0)
			return y;
		if (y.sign == 0)
			return x;
		if (x.sign == POS_INF || x.sign == NEG_INF)
			return (y.sign == -x.sign) ? Zero : x;
		if (y.sign == POS_INF || y.sign == NEG_INF)
			return y;

		return (x.sign == y.sign) ? uadd(x, y) : usub(x, y);
	}

	public static Dnum sub(Dnum x, Dnum y) {
		if (x.sign == 0)
			return y.negate();
		if (y.sign == 0)
			return x;
		if (x.sign == POS_INF || x.sign == NEG_INF)
			return (y.sign == x.sign) ? Zero : x;
		if (y.sign == POS_INF)
			return MinusInf;
		if (y.sign == NEG_INF)
			return Inf;

		return (x.sign == y.sign) ? usub(x, y) : uadd(x, y);
	}

	/** unsigned add */
	private static Dnum uadd(Dnum x, Dnum y) {
		if (x.exp > y.exp) {
			Align y2 = new Align(y);
			if (! align(x, y2))
				return x;
			else
				return from(x.sign, x.coef + y2.coef, x.exp);
		} else if (x.exp < y.exp) {
			Align x2 = new Align(x);
			if (! align(y, x2))
				return y;
			else
				return from(x.sign, x2.coef + y.coef, x2.exp);
		} else
			return from(x.sign, x.coef + y.coef, x.exp);
	}

	/** unsigned subtract */
	private static Dnum usub(Dnum x, Dnum y) {
		if (x.exp > y.exp) {
			Align y2 = new Align(y);
			return align(x, y2) ? from(x.sign, x.coef - y2.coef, x.exp) : x;

		} else if (x.exp < y.exp) {
			Align x2 = new Align(x);
			return align(y, x2)
					? from(-x.sign, y.coef - x2.coef, x2.exp)
					: y.negate();
		} else
			return x.coef > y.coef
					? from(x.sign, x.coef - y.coef, x.exp)
					: from(-x.sign, y.coef - x.coef, x.exp);
	}

	/** modifies y */
	private static boolean align(Dnum x, Align y) {
		int yshift = ilog10(y.coef);
		int e = x.exp - y.exp;
		if (e > yshift)
			return false;
		yshift = e;
		y.coef = (y.coef + halfpow10[yshift]) / pow10[yshift];
		y.exp += yshift;
		return true;
	}

	/** mutable */
	private static class Align {
		long coef;
		byte exp;

		Align(Dnum dn) {
			this.coef = dn.coef;
			this.exp = dn.exp;
		}
	}

	// multiply ----------------------------------------------------------------

	private static final long E7 = 10_000_000L;

	public static Dnum mul(Dnum x, Dnum y) {
		int sign = (x.sign * y.sign);
		if (sign == 0)
			return Zero;
		else if (x.isInf() || y.isInf())
			return inf(sign);
		int e = x.exp + y.exp;

		// split unevenly to use full 64 bit range to get more precision
		// and avoid needing xlo * ylo
		long xhi = x.coef / E7; // 9 digits
		long xlo = x.coef % E7; // 7 digits
		long yhi = y.coef / E7; // 9 digits
		long ylo = y.coef % E7; // 7 digits

		long c = xhi * yhi + (xlo * yhi + ylo * xhi) / E7;
		return from(sign, c, e - 2);
	}

	// divide ------------------------------------------------------------------

	public static Dnum div(Dnum x, Dnum y) {
		int sign = x.sign * y.sign;
		if (sign == 0)
			return x.isZero() ? Zero : /* y.isZero() */ inf(x.sign);
		if (x.isInf())
			return y.isInf()
					? sign < 0 ? MinusOne : One
					: inf(sign);
		if (y.isInf())
			return Zero;

		long q = Div128.divide(x.coef, y.coef);
		return from(sign, q, x.exp - y.exp);
	}

// -------------------------------------------------------------------------

	private static Dnum inf(int sign) {
		return sign < 0 ? MinusInf : sign > 0 ? Inf : Zero;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (coef ^ (coef >>> 32));
		result = prime * result + exp;
		result = prime * result + sign;
		return result;
	}

	/** Handles Integer */
	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (other == null)
			return false;
		Dnum that;
		if (other instanceof Integer)
			that = Dnum.from((int) other);
		else if (getClass() != other.getClass())
			return false;
		else
			that = (Dnum) other;
		return sign == that.sign && exp == that.exp && coef == that.coef;
	}

	// for tests, rounds off last digit
	static boolean almostSame(Dnum x, Dnum y)
		{
		return x.sign == y.sign && x.exp == y.exp &&
				(x.coef / 10) == (y.coef / 10);
		}

	@Override
	public int compareTo(Dnum that) {
		return cmp(this, that);
	}

	public static int cmp(Dnum x, Dnum y) {
		if (x.sign > y.sign)
			return +1;
		else if (x.sign < y.sign)
			return -1;
		int sign = x.sign;
		if (sign == 0 || sign == NEG_INF || sign == POS_INF)
			return 0;
		if (x.exp < y.exp)
			return -sign;
		if (x.exp > y.exp)
			return +sign;
		return sign * Long.compare(x.coef, y.coef);
	}

	/** @return true if it's an integer, but may not fit in int or long */
	public boolean integral() {
		if (sign == 0)
			return true;
		if (sign == NEG_INF || sign == POS_INF || exp <= 0) // e.g. .1
			return false;
		if (exp >= MAX_DIGITS) // e.g. 1e99
			return true;
		return (coef % pow10[MAX_DIGITS - exp]) == 0;
	}

	public Dnum integer() {
		return integer(RoundingMode.DOWN);
	}

	/** @return The integer part of the value i.e. truncating the fractional.
	 *  But won't necessarily fit in int or long */
	public Dnum integer(RoundingMode mode) {
		if (sign == 0 || sign == NEG_INF || sign == POS_INF || exp >= MAX_DIGITS)
			return this;
		if (exp <= 0) {
			if (mode == RoundingMode.UP ||
					(mode == RoundingMode.HALF_UP && exp == 0 && coef >= One.coef * 5))
				return new Dnum(sign, One.coef, exp + 1);
			return Zero;
		}
		int e = MAX_DIGITS - exp;
		long frac = coef % pow10[e];
		if (frac == 0)
			return this;
		long i = coef - frac;
		if ((mode == RoundingMode.UP && frac > 0) ||
				(mode == RoundingMode.HALF_UP && frac >= halfpow10[e]))
			return Dnum.from(sign, i + pow10[e], exp); // normalize
		return new Dnum(sign, i, exp);
	}

	/** @return The integer part of the value i.e. truncating the fractional.
	 *  But won't necessarily fit in int or long */
	public Dnum frac() {
		if (sign == 0 || sign == NEG_INF || sign == POS_INF || exp >= MAX_DIGITS)
			return Zero;
		if (exp <= 0)
			return this;
		long frac = coef % pow10[MAX_DIGITS - exp];
		return frac == coef ? this : Dnum.from(sign, frac, exp);
	}

	/** Throws if value doesn't fit in int */
	@Override
	public int intValue() {
		long n = longValue();
		if (Integer.MIN_VALUE <= n && n <= Integer.MAX_VALUE)
			return (int) n;
		throw new RuntimeException("can't convert number to integer");
	}

	/** Throws if value doesn't fit in long */
	@Override
	public long longValue() {
		if (sign == ZERO)
			return 0;
		if (sign != NEG_INF && sign != POS_INF) {
			if (0 < exp && exp < MAX_DIGITS &&
					(coef % pow10[MAX_DIGITS - exp]) == 0)
				return sign * (coef / pow10[MAX_DIGITS - exp]); // usual case
			if (exp == MAX_DIGITS)
				return sign * coef;
			if (exp == MAX_DIGITS + 1)
				return sign * (coef * 10);
			if (exp == MAX_DIGITS + 2)
				return sign * (coef * 100);
			if (exp == MAX_DIGITS + 3 && coef < Long.MAX_VALUE / 1000)
				return sign * (coef * 1000);
		}
		throw new RuntimeException("can't convert number to integer");
	}

	/** @return integer value if in range, else Integer.MIN_VALUE */
	public int intOrMin() {
		if (sign == ZERO)
			return 0;
		if (sign != NEG_INF && sign != POS_INF &&
				0 < exp && exp <= 10 &&
				(coef % pow10[MAX_DIGITS - exp]) == 0) {
			long n = sign * (coef / pow10[MAX_DIGITS - exp]);
			if (Integer.MIN_VALUE < n && n <= Integer.MAX_VALUE)
				return (int) n;
		}
		return Integer.MIN_VALUE;
	}

	/** @return integer value if in range, else null */
	public Object intObject() {
		if (sign == ZERO)
			return 0;
		if (sign != NEG_INF && sign != POS_INF &&
				0 < exp && exp <= 10 &&
				(coef % pow10[MAX_DIGITS - exp]) == 0) {
			long n = sign * (coef / pow10[MAX_DIGITS - exp]);
			if (Integer.MIN_VALUE <= n && n <= Integer.MAX_VALUE)
				return (int) n;
		}
		return null;
	}

	@Override
	public float floatValue() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double doubleValue() { // needed for math/trig methods
		switch (sign) {
		case ZERO:
			return 0.0;
		case NEG_INF:
			return Double.NEGATIVE_INFINITY;
		case POS_INF:
			return Double.POSITIVE_INFINITY;
		}
		int e = exp - MAX_DIGITS;
		return sign * (e < 0 ? coef / dpow10[-e] : coef * dpow10[e]);
	}

	private static final double dpow10[] = {
		1e0, 1e1, 1e2, 1e3, 1e4, 1e5, 1e6, 1e7, 1e8, 1e9, 1e10, 1e11, 1e12,
		1e13, 1e14, 1e15, 1e16, 1e17, 1e18, 1e19, 1e20, 1e21, 1e22, 1e23,
		1e24, 1e25, 1e26, 1e27, 1e28, 1e29, 1e30, 1e31, 1e32, 1e33, 1e34,
		1e35, 1e36, 1e37, 1e38, 1e39, 1e40, 1e41, 1e42, 1e43, 1e44, 1e45,
		1e46, 1e47, 1e48, 1e49, 1e50, 1e51, 1e52, 1e53, 1e54, 1e55, 1e56,
		1e57, 1e58, 1e59, 1e60, 1e61, 1e62, 1e63, 1e64, 1e65, 1e66, 1e67,
		1e68, 1e69, 1e70, 1e71, 1e72, 1e73, 1e74, 1e75, 1e76, 1e77, 1e78,
		1e79, 1e80, 1e81, 1e82, 1e83, 1e84, 1e85, 1e86, 1e87, 1e88, 1e89,
		1e90, 1e91, 1e92, 1e93, 1e94, 1e95, 1e96, 1e97, 1e98, 1e99, 1e100,
		1e101, 1e102, 1e103, 1e104, 1e105, 1e106, 1e107, 1e108, 1e109, 1e110,
		1e111, 1e112, 1e113, 1e114, 1e115, 1e116, 1e117, 1e118, 1e119, 1e120,
		1e121, 1e122, 1e123, 1e124, 1e125, 1e126, 1e127, 1e128, 1e129, 1e130,
		1e131, 1e132, 1e133, 1e134, 1e135, 1e136, 1e137, 1e138, 1e139, 1e140,
		1e141, 1e142, 1e143, 1e144
	};

	/**
	 * r >= 0 means the specified number of decimal places.
	 * r < 0 means abs(r) number of 0's to the left of the decimal place.
	 * @return A new Dnum.
	 */
	public Dnum round(int r, RoundingMode mode) {
		if (isZero() || isInf() || r >= MAX_DIGITS)
			return this;
		if (r <= -MAX_DIGITS)
			return Zero;
		Dnum n = new Dnum(sign, coef, exp + r); // multiply by 10^r
		n = n.integer(mode);
		if (n.sign == POS || n.sign == NEG) // i.e. not zero or inf
			return new Dnum(n.sign, n.coef, n.exp - r);
		return n;
	}

	public Dnum round(int r) {
		return round(r, RoundingMode.HALF_UP);
	}

}
