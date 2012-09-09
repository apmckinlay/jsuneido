/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import static suneido.language.Numbers.*;

import java.math.BigDecimal;

import suneido.language.*;

// assert self instanceof Number
/**
 * Methods for numbers.
 * <li>Attempts to handle all numeric types: Byte, Short, Int, Long, Float, Double,
 * BigInteger, BigDecimal.
 * <li>Prefers to use decimal types (i.e. not float or double)
 * <li>Prefers to return results as Int or Long or BigDecimal.</li>
 * <p>WARNING: Some operations will not work if integer precision greater than long.
 */
public class NumberMethods extends BuiltinMethods {
	public static final NumberMethods singleton = new NumberMethods();

	private NumberMethods() {
		super(NumberMethods.class, "Numbers");
	}

	public static class Frac extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			if (integral(self))
				return 0;
			Number n = (Number) self;
			if (self instanceof Float || self instanceof Double)
				return n.doubleValue() % 1;
			BigDecimal bd = (BigDecimal) self;
			return bd.remainder(BigDecimal.ONE);
		}
	}

	public static class Int extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			if (integral(self))
				return self;
			return Numbers.narrow(((Number) self).longValue());
		}
	}

	public static class Hex extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return Long.toHexString(((Number) self).longValue());
		}
	}

	public static class Chr extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			long n = ((Number) self).longValue();
			return Character.toString((char) (n & 0xff));
		}
	}

	public static class Format extends SuMethod1 {
		{ params = new FunctionSpec("mask"); }
		@Override
		public Object eval1(Object self, Object a) {
			BigDecimal n = toBigDecimal(self);
			String mask = Ops.toStr(a);
			return format(mask, n);
		}
	}

	private static final BigDecimal half = new BigDecimal(".5");

	static String format(String mask, BigDecimal n) {
		BigDecimal x = n.abs();

		int masksize = mask.length();
		String num = "";
		if (n.equals(BigDecimal.ZERO)) {
			int i = mask.indexOf('.');
			if (i != -1) {
				StringBuilder sb = new StringBuilder(8);
				for (++i; i < masksize && mask.charAt(i) == '#'; ++i)
					sb.append('0');
				num = sb.toString();
			}
		} else {
			int i = mask.indexOf('.');
			if (i != -1)
				for (++i; i < masksize && mask.charAt(i) == '#'; ++i)
					x = x.movePointRight(1);
			BigDecimal tmp = new BigDecimal(x.toBigInteger());
			if (x != tmp) { // need to round
				x = x.add(half);
				x = new BigDecimal(x.toBigInteger());
			}
			num = Ops.toStringBD(x);
		}
		StringBuilder dst = new StringBuilder();
		int sign = n.signum();
		boolean signok = (sign >= 0);
		int i, j;
		for (j = num.length() - 1, i = masksize - 1; i >= 0; --i) {
			char c = mask.charAt(i);
			switch (c) {
			case '#':
				dst.append(j >= 0 ? num.charAt(j--) : '0');
				break;
			case ',':
				if (j >= 0)
					dst.append(',');
				break;
			case '-':
			case '(':
				signok = true;
				if (sign < 0)
					dst.append(c);
				break;
			case ')':
				dst.append(sign < 0 ? c : ' ');
				break;
			case '.':
			default:
				dst.append(c);
				break;
			}
		}
		dst.reverse();

		// strip leading zeros
		int start = 0;
		while (dst.charAt(start) == '-' || dst.charAt(start) == '(')
			++start;
		int end = start;
		while (dst.charAt(end) == '0' && end + 1 < dst.length())
			++end;
		dst.delete(start, end);

		if (j >= 0)
			return "#"; // too many digits for mask
		if (!signok)
			return "-"; // negative not handled by mask
		return dst.toString();
	}

	// used by stdlib Numbers Round so commonly called
	public static class Pow extends SuMethod1 {
		{ params = new FunctionSpec("number"); }
		@Override
		public Object eval1(Object self, Object a_) {
			Number sn = (Number) self;
			Number an = toNum(a_);
			if (isZero(an))
				return 1;
			if (isZero(sn))
				return 0;
			if (longable(an)) {
				long ai = an.longValue();
				if (ai == 1)
					return self;
				if (longable(sn)) {
					long si = sn.longValue();
					if (si == 1)
						return 1;
					if (0 < ai && ai < 16) {
						long result = 1;
						for (int i = 0; i < ai; ++i)
							result *= si;
						return narrow(result);
					}
				}
				if (Math.abs(ai) < Integer.MAX_VALUE)
					return toBigDecimal(self).pow((int) ai, MC);
			}
			return narrow(Math.pow(sn.doubleValue(), an.doubleValue()));
		}
	}

	public static class Cos extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			double d = ((Number) self).doubleValue();
			return Math.cos(d);
		}
	}

	public static class Sin extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			double d = ((Number) self).doubleValue();
			return Math.sin(d);
		}
	}

	public static class Tan extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			double d = ((Number) self).doubleValue();
			return Math.tan(d);
		}
	}

	public static class ACos extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			double d = ((Number) self).doubleValue();
			return Math.acos(d);
		}
	}

	public static class ASin extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			double d = ((Number) self).doubleValue();
			return Math.asin(d);
		}
	}

	public static class ATan extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			double d = ((Number) self).doubleValue();
			return Math.atan(d);
		}
	}

	public static class Exp extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			double d = ((Number) self).doubleValue();
			return Math.exp(d);
		}
	}

	public static class Log extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			double d = ((Number) self).doubleValue();
			return Math.log(d);
		}
	}

	public static class Log10 extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			double d = ((Number) self).doubleValue();
			return Math.log10(d);
		}
	}

	public static class Sqrt extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			double d = ((Number) self).doubleValue();
			return Math.sqrt(d);
		}
	}
	
}
