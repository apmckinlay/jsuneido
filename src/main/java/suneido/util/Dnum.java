/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

/**
 * Decimal floating point number implementation
 * using a 64 bit long (treated as unsigned) for the coefficient.
 * <p>
 * Value is sign * coef * 10^exp, zeroed value is 0.
 * <p>
 * Immutable as far as public methods, but mutable instances used internally.
 */
public class Dnum implements Comparable<Dnum> {
	private long coef; // mostly final, only modified internally
	private final byte sign;
	private byte exp; // mostly final, only modified internally

	final static byte PLUS = +1;
	final static byte ZERO = 0;
	final static byte MINUS = -1;
	final static byte expInf = Byte.MAX_VALUE;
	final static long expCoef = ~0L;

	final static Dnum Zero = new Dnum(ZERO, 0, 0);
	final static Dnum One = new Dnum(PLUS, 1, 0);
	final static Dnum Inf = new Dnum(PLUS, expCoef, expInf);
	final static Dnum MinusInf = new Dnum(MINUS, expCoef, expInf);

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

	private static final CharMatcher cm_zero = CharMatcher.is('0');

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
		digits = cm_zero_or_dot.trimTrailingFrom(digits);
		return signStr + digits + expStr;
	}

	private static final CharMatcher cm_zero_or_dot = CharMatcher.anyOf("0.");

	boolean isZero() {
		return sign == ZERO;
	}

	public Dnum neg() {
		return new Dnum((byte) -sign, coef, exp);
	}

	public Dnum abs() {
		return sign == MINUS ? new Dnum(PLUS, coef, exp) : this;
	}

	public Dnum add(Dnum y) {
		return add(this, y);
	}

	public static Dnum add(Dnum x, Dnum y) {
		if (x.isZero())
			return y;
		else if (y.isZero())
			return x;
		else if (x.equals(Inf))
			if (y.equals(MinusInf))
				return Zero;
			else
				return Inf;
		else if (x.equals(MinusInf))
			if (y.equals(Inf))
				return Zero;
			else
				return MinusInf;
		else if (y.equals(Inf))
			return Inf;
		else if (y.equals(MinusInf))
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
		else if (x.equals(Inf))
			if (y.equals(Inf))
				return Zero;
			else
				return Inf;
		else if (x.equals(MinusInf))
			if (y.equals(MinusInf))
				return Zero;
			else
				return MinusInf;
		else if (y.equals(Inf))
			return MinusInf;
		else if (y.equals(MinusInf))
			return Inf;
		else if (x.sign != y.sign)
			return uadd(x, y);
		else
			return usub(x, y);
	}

	private static Dnum uadd(Dnum x, Dnum y) {
		if (x.exp != y.exp) {
			x = x.copy();
			y = y.copy();
			align(x, y);
		}
		// align may make coef 0 if exp is too different
		if (x.coef == 0)
			return new Dnum(x.sign, y.coef, y.exp);
		else if (y.coef == 0)
			return new Dnum(y.sign, x.coef, x.exp);
		long coef = x.coef + y.coef;
		if (coef < x.coef || coef < y.coef) { // overflow
			Bool roundup = new Bool(false);
			x.shiftRight(roundup);
			if (roundup.value)
				x.coef++;
			y.shiftRight(roundup);
			if (roundup.value)
				y.coef++;
			coef = x.coef + y.coef;
		}
		return result(coef, x.sign, x.exp);
	}

	private static Dnum usub(Dnum x, Dnum y) {
		if (x.exp != y.exp) {
			x = x.copy();
			y = y.copy();
			align(x, y);
		}
		// TODO Auto-generated method stub
		return null;
	}

	// WARNING: may modify x and/or y - requires defensive copies
	private static void align(Dnum x, Dnum y) {
		if (x.exp > y.exp) {
			Dnum tmp = x;
			x = y;
			y = tmp;
		}
		while (y.exp > x.exp && y.shiftLeft()) {
		}
		Bool roundup = new Bool(false);
		while (y.exp > x.exp && x.shiftRight(roundup)) {
		}
		if (roundup.value)
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
		final long HI4 = 0xf << 60;
		return (n & HI4) == 0;
	}

	/** WARNING: may modify, requires defensive copy */
	private boolean shiftRight(Bool roundup) {
		roundup.value = false;
		if (coef == 0)
			return false;
		roundup.value = (coef % 10) >= 5;
		coef /= 10;
		// don't increment past max
		if (exp < Byte.MAX_VALUE)
			exp++;
		return true;
	}

	private static class Bool {
		boolean value;
		Bool(boolean value) {
			this.value = value;
		}
	}

	private static Dnum result(long coef, byte sign, int exp) {
		if (exp >= expInf)
			return inf(sign);
		else if (exp < Byte.MIN_VALUE || coef == 0)
			return Zero;
		else
			return new Dnum(sign, coef, exp);
	}

	// -------------------------------------------------------------------------

	private static Dnum inf(byte sign2) {
		// TODO Auto-generated method stub
		return null;
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

	@Override
	public int compareTo(Dnum other) {
		if (sign > other.sign)
			return +1;
		else if (sign < other.sign)
			return -1;
		// TODO subtract and compare result to zero
		return -1;
	}

	Dnum check() {
		if (sign != PLUS && sign != ZERO && sign != MINUS)
			throw new RuntimeException("Dnum invalid sign " + sign);
		if (sign == ZERO && coef != 0)
			throw new RuntimeException("Dnum sign is zero but coef is " + coef);
		if (sign != ZERO && coef == 0)
			throw new RuntimeException("Dnum coef is zero but sign is " + sign);
		return this;
	}

}
