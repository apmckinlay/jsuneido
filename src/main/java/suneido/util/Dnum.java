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
 * Value is -1^sign * coef * 10^exp, zeroed value is 0.
 */
public class Dnum implements Comparable<Dnum> {
	private final long coef;
	private final byte sign;
	private final byte exp;

	final static byte PLUS = 0;
	final static byte MINUS = 1;
	final static byte expInf = Byte.MAX_VALUE;

	final static Dnum Zero = new Dnum(PLUS, 0, 0);
	final static Dnum One = new Dnum(PLUS, 1, 0);
	final static Dnum Inf = new Dnum(PLUS, 0, expInf);
	final static Dnum MinusInf = new Dnum(MINUS, 0, expInf);

	Dnum(byte sign, long coef, int exp) {
		this.coef = coef;
		this.sign = sign;
		assert Byte.MIN_VALUE <= exp && exp <= Byte.MAX_VALUE;
		this.exp = (byte) exp;
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
		return coef == 0 && exp != expInf;
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
		if (sign != other.sign)
			return sign == PLUS ? +1 : -1;
		// TODO subtract and compare result to zero
		return -1;
	}

}
