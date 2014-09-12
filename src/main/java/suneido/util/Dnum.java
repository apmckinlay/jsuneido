/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.common.primitives.UnsignedLongs;

/**
 * Decimal floating point number implementation
 * using a 64 bit long (treated as unsigned) for the coefficient.
 * <p>
 * Value is sign * coef * 10^exp, zeroed value is 0.
 * If sign is 0 then coef should be 0.
 * Infinite is represented by the maximum exponent (expInf = Byte.MAX_VALUE = 127).
 * If exp is expInf then coef should be the maximum (coefInf = Long.MAX_VALUE).
 * <p>
 * Values are not "normalized" e.g. after parse or math operations.
 * so "equal" values may have different representations
 * e.g. 1e3 or 10e2 or 1000
 * compareTo handles this properly and equals is defined in terms of compareTo.
 * toString will generally give the same result regardless of representation.
 * <p>
 * Math "sticks" at infinite when it overflows.
 * <p>
 * There is no NaN, inf / inf = 1, 0 / ... = 0, inf / ... = inf
 * <p>
 * Immutable as far as public methods, but mutable instances used internally.
 */
public class Dnum implements Comparable<Dnum> { // TODO extend Number ???
	private long coef;
	private final byte sign;
	private byte exp;

	final static byte PLUS = +1;
	final static byte ZERO = 0;
	final static byte MINUS = -1;
	final static byte expInf = Byte.MAX_VALUE;
	final static long coefInf = UnsignedLongs.MAX_VALUE;

	final static Dnum Zero = new Dnum(ZERO, 0, 0);
	final static Dnum One = new Dnum(PLUS, 1, 0);
	final static Dnum Inf = new Dnum(PLUS, coefInf, expInf);
	final static Dnum MinusInf = new Dnum(MINUS, coefInf, expInf);

	Dnum(byte sign, long coef, int exp) {
		this.coef = coef;
		this.sign = sign;
		assert Byte.MIN_VALUE <= exp && exp <= Byte.MAX_VALUE;
		this.exp = (byte) exp;
	}

	private Dnum copy() {
		return new Dnum(sign, coef, exp);
	}

	public static Dnum parse(String s) {
		if (s == null)
			throw new RuntimeException("cannot convert null to Dnum");
		if (s.length() < 1)
			throw new RuntimeException("cannot convert empty string to Dnum");
		if (s.equals("0")) {
			return Zero;
		}
		byte sign = PLUS;
		int i = 0;
		if (s.charAt(i) == '+') {
			i++;
		} else if (s.charAt(i) == '-') {
			sign = MINUS;
			i++;
		}
		String before = spanDigits(s.substring(i));
		i += before.length();
		String after = "";
		if (i < s.length() && s.charAt(i) == '.') {
			i++;
			after = spanDigits(s.substring(i));
			i += after.length();
		}
		after = cm_zero.trimTrailingFrom(after);
		long coef = Long.parseUnsignedLong(before + after);

		int exp = 0;
		if (i < s.length() && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
			exp = Integer.parseInt(s.substring(++i));
		}
		if (coef == 0) {
			return Zero;
		}
		exp -= after.length();
		if (exp < -127 || exp >= 127) {
			throw new RuntimeException("exponent out of range (" + s + ")");
		}
		return new Dnum(sign, coef, exp);
	}

	private static String spanDigits(String s) {
		int i = 0;
		while (i < s.length() && '0' <= s.charAt(i) && s.charAt(i) <= '9')
			++i;
		return s.substring(0, i);
	}

	@Override
	public String toString() {
		if (isZero()) {
			return "0";
		}
		String signStr = "";
		if (sign == MINUS) {
			signStr = "-";
		}
		if (exp == expInf) {
			return signStr + "inf";
		}
		String digits = Long.toUnsignedString(coef);
		if (0 <= exp && exp <= 4) {
			// decimal to the right
			digits += Strings.repeat("0", exp);
			return signStr + digits;
		}
		String expStr = "";
		if (-digits.length()-4 < exp && exp <= -digits.length()) {
			// decimal to the left
			digits = "." + Strings.repeat("0", -exp-digits.length()) + digits;
		} else if (-digits.length() < exp && exp <= -1) {
			// decimal within
			int i = digits.length() + exp;
			digits = digits.substring(0, i) + "." + digits.substring(i);
		} else {
			// use scientific notation
			int e = exp + digits.length() - 1;
			digits = digits.substring(0, 1) + "." + digits.substring(1);
			expStr = "e" + e;
		}
		digits = cm_zero.trimTrailingFrom(digits);
		digits = cm_dot.trimTrailingFrom(digits);
		return signStr + digits + expStr;
	}

	private static final CharMatcher cm_zero = CharMatcher.is('0').precomputed();
	private static final CharMatcher cm_dot = CharMatcher.is('.').precomputed();

	public boolean isZero() {
		return sign == ZERO;
	}

	/** @return true if plus or minus infinite */
	public boolean isInf() {
		return exp == expInf;
	}

	public Dnum neg() {
		return new Dnum((byte) -sign, coef, exp);
	}

	public Dnum abs() {
		return sign == MINUS ? new Dnum(PLUS, coef, exp) : this;
	}

	public int signum() {
		return sign;
	}

	public int exp() {
		return exp;
	}

	public long coef() {
		return coef;
	}

	/** returns a new Dnum without trailing zero digits */
	public Dnum stripTrailingZeros() {
		Dnum x = copy();
		while (Long.remainderUnsigned(x.coef, 10) == 0) {
			x.coef = Long.divideUnsigned(x.coef, 10);
			x.exp--;
		}
		return x;
	}

	// add and subtract --------------------------------------------------------

	public Dnum add(Dnum y) {
		return add(this, y);
	}

	public static Dnum add(Dnum x, Dnum y) {
		if (x.isZero())
			return y;
		else if (y.isZero())
			return x;
		else if (x.is(Inf))
			if (y.is(MinusInf))
				return Zero;
			else
				return Inf;
		else if (x.is(MinusInf))
			if (y.is(Inf))
				return Zero;
			else
				return MinusInf;
		else if (y.is(Inf))
			return Inf;
		else if (y.is(MinusInf))
			return MinusInf;
		else if (x.sign != y.sign)
			return usub(x, y);
		else
			return uadd(x, y);
	}

	public Dnum sub(Dnum y) {
		return sub(this, y);
	}

	public static Dnum sub(Dnum x, Dnum y) {
		if (x.isZero())
			return y.neg();
		else if (y.isZero())
			return x;
		else if (x.is(Inf))
			if (y.is(Inf))
				return Zero;
			else
				return Inf;
		else if (x.is(MinusInf))
			if (y.is(MinusInf))
				return Zero;
			else
				return MinusInf;
		else if (y.is(Inf))
			return MinusInf;
		else if (y.is(MinusInf))
			return Inf;
		else if (x.sign != y.sign)
			return uadd(x, y);
		else
			return usub(x, y);
	}

	/** unsigned add */
	private static Dnum uadd(Dnum x, Dnum y) {
		if (x.exp != y.exp) {
			x = x.copy();
			y = y.copy();
			align(x, y);
		}
		long coef = x.coef + y.coef;
		if (Long.compareUnsigned(coef, x.coef) < 0 ||
				Long.compareUnsigned(coef, y.coef) < 0) { // overflow
			boolean[] roundup = new boolean[1];
			x.shiftRight(roundup);
			if (roundup[0])
				x.coef++;
			y.shiftRight(roundup);
			if (roundup[0])
				y.coef++;
			coef = x.coef + y.coef;
		}
		return result(coef, x.sign, x.exp);
	}

	/** unsigned subtract */
	private static Dnum usub(Dnum x, Dnum y) {
		if (x.exp != y.exp) {
			x = x.copy();
			y = y.copy();
			align(x, y);
		}
		byte sign = x.sign;
		if (Long.compareUnsigned(x.coef, y.coef) < 0) {
			Dnum tmp = x; x = y; y = tmp; // swap
			sign *= -1; // flip sign
		}
		return result(x.coef - y.coef, sign, x.exp);
	}

	/** WARNING: may modify x and/or y - requires defensive copies */
	private static void align(Dnum x, Dnum y) {
		if (x.exp > y.exp) {
			Dnum tmp = x; x = y; y = tmp; // swap
		}
		while (y.exp > x.exp && y.shiftLeft()) {
		}
		boolean[] roundup = new boolean[1];
		while (y.exp > x.exp && x.shiftRight(roundup)) {
		}
		if (x.exp != y.exp) {
			assert x.coef == 0;
			x.exp = y.exp;
		} else if (roundup[0])
			x.coef++;
	}

	/** WARNING: may modify, requires defensive copy */
	private boolean shiftLeft() {
		if (! mul10safe(coef))
			return false;
		coef *= 10;
		// don't decrement past min
		if (exp > Byte.MIN_VALUE)
			exp--;
		return true;
	}

	private static boolean mul10safe(long n) {
		final long HI4 = 0xfL << 60;
		return (n & HI4) == 0;
	}

	/** WARNING: may modify, requires defensive copy */
	private boolean shiftRight(boolean[] roundup) {
		roundup[0] = false;
		if (coef == 0)
			return false;
		roundup[0] = Long.remainderUnsigned(coef, 10) >= 5;
		coef = Long.divideUnsigned(coef, 10);
		// don't increment past max
		if (exp < Byte.MAX_VALUE)
			exp++;
		return true;
	}

	private static Dnum result(long coef, byte sign, int exp) {
		if (exp >= expInf)
			return inf(sign);
		else if (exp < Byte.MIN_VALUE || coef == 0)
			return Zero;
		else
			return new Dnum(sign, coef, exp);
	}

	// multiply ----------------------------------------------------------------

	public Dnum mul(Dnum y) {
		return mul(this, y);
	}

	public static Dnum mul(Dnum x, Dnum y) {
		byte sign = (byte) (x.sign * y.sign);
		if (x == One)
			return y;
		else if (y == One)
			return x;
		else if (x == Zero || y == Zero)
			return Zero;
		else if (x.isInf() || y.isInf())
			return result(0, sign, expInf);
		x = minCoef(x);
		y = minCoef(y);
		if (Long.numberOfLeadingZeros(x.coef) +
				Long.numberOfLeadingZeros(y.coef) >= 64)
			// coef won't overflow, common fast path
			return result(x.coef * y.coef, sign, x.exp + y.exp);
		// drop 5 least significant bits off x and y
		// 59 bits < 18 decimal digits
		// 32 bits > 9 decimal digits
		// so we can split x & y into two pieces
		// and multiply separately guaranteed not to overflow
		long[] xs = split(x);
		long[] ys = split(y);
		int exp = x.exp + y.exp;
		Dnum r1 = result(xs[0] * ys[0], sign, exp);
		Dnum r2 = result(xs[0] * ys[1], sign, exp + 9);
		Dnum r3 = result(xs[1] * ys[0], sign, exp + 9);
		Dnum r4 = result(xs[1] * ys[1], sign, exp + 18);
		return add(r1, add(r2, add(r3, r4)));
	}

	private static Dnum minCoef(Dnum x) {
		if (x.coef != 0 && Long.remainderUnsigned(x.coef, 10) == 0) {
			x = x.copy();
			boolean[] roundup = new boolean[1];
			do
				x.shiftRight(roundup);
			while (x.coef != 0 && Long.remainderUnsigned(x.coef, 10) == 0);
			if (roundup[0])
				x.coef++;
		}
		return x;
	}

	private static long[] split(Dnum x) {
		final long HI5 = 0x1fL << 59;
		if ((x.coef & HI5) != 0) {
			x = x.copy();
			boolean[] roundup = new boolean[1];
			do
				x.shiftRight(roundup);
			while ((x.coef & HI5) != 0);
			if (roundup[0])
				x.coef++;
		}
		final long oneE9 = 1000000000L;
		return new long[] { Long.remainderUnsigned(x.coef, oneE9),
				Long.divideUnsigned(x.coef, oneE9) };
	}

	// divide ------------------------------------------------------------------

	public Dnum div(Dnum y) {
		return div(this, y);
	}

	public static Dnum div(Dnum x, Dnum y) {
		if (x.isZero())
			return Zero;
		else if (y.isZero())
			return inf(x.sign);
		else if (x.isInf()) {
			byte sign = (byte) (x.sign * y.sign);
			return y.isInf() ? new Dnum(sign, 1, 0) : inf(sign);
		} else if (y.isInf())
			return Zero;
		return div2(x, y);
	}

	private static Dnum div2(Dnum x, Dnum y) {
		byte sign = (byte) (x.sign * y.sign);
		int exp = x.exp - y.exp;
		long xcoef = x.coef;
		long ycoef = y.coef;
		// strip trailing zeroes from y i.e. shift right as far as possible
		while (Long.remainderUnsigned(ycoef, 10) == 0) {
			ycoef = Long.divideUnsigned(ycoef, 10);
			exp--;
		}
		long z = 0;
		loop: while (xcoef != 0) {
			// shift x left until divisible or as far as possible
			while (Long.remainderUnsigned(xcoef, ycoef) != 0 &&
					mul10safe(xcoef) && mul10safe(z)) {
				xcoef *= 10;
				z *= 10;
				exp--;
			}
			while (Long.compareUnsigned(xcoef, ycoef) < 0) { // xcoef < ycoef
				if (! mul10safe(z))
					break loop;
				ycoef = Long.divideUnsigned(ycoef, 10);
				z *= 10;
				exp--;
			}
			long q = Long.divideUnsigned(xcoef, ycoef);
			if (q == 0) {
				break;
			}
			z += q;
			xcoef = Long.remainderUnsigned(xcoef, ycoef);
		}
		return result(z, sign, exp);
	}

	// -------------------------------------------------------------------------

	private static Dnum inf(byte sign) {
		assert sign == PLUS || sign == MINUS;
		return sign == PLUS ? Inf : MinusInf;
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

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Dnum other = (Dnum) obj;
		if (coef == other.coef && exp == other.exp && sign == other.sign)
			return true;
		return 0 == compareTo(other);
	}

	private boolean is(Dnum other) {
		return coef == other.coef && exp == other.exp && sign == other.sign;
	}

	public static int cmp(Dnum x, Dnum y) {
		return x.compareTo(y);
	}

	@Override
	public int compareTo(Dnum other) {
		if (sign > other.sign)
			return +1;
		else if (sign < other.sign)
			return -1;
		return sub(other).sign;
	}

	Dnum check() {
		assert sign == PLUS || sign == ZERO || sign == MINUS :
			"Dnum invalid sign " + sign;
		assert sign != ZERO || coef == 0 :
			"Dnum sign is zero but coef is " + coef;
		assert sign == ZERO || coef != 0 :
			"Dnum coef is zero but sign is " + sign;
		assert exp != expInf || coef == coefInf :
			"Dnum exp is inf but coef is " + coef;
		return this;
	}

}
