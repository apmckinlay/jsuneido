/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language.builtin;

import java.math.BigDecimal;
import java.math.BigInteger;

import suneido.SuException;
import suneido.language.*;

// self instanceof Number
public class NumberMethods extends PrimitiveMethods {
	public static final NumberMethods singleton = new NumberMethods();

	private NumberMethods() {
		super("Number", NumberMethods.class);
	}

	private static BigDecimal toBigDecimal(Object n) {
		if (n instanceof BigDecimal)
			return (BigDecimal) n;
		if (n instanceof Integer || n instanceof Long || n instanceof Short ||
				n instanceof Byte)
			return BigDecimal.valueOf(((Number) n).longValue());
		if (n instanceof Float || n instanceof Double)
			return BigDecimal.valueOf(((Number) n).doubleValue());
		if (n instanceof BigInteger)
			return new BigDecimal((BigInteger) n);
		throw SuException.unreachable();
	}

	private static boolean isInt(Object n) {
		return n instanceof Integer || n instanceof Long ||
				n instanceof Short || n instanceof Byte;
	}

	public static class Frac extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			if (isInt(self) || self instanceof BigInteger)
				return 0;
			return Ops.sub(self, trunc(self));
		}
	}

	public static class Int extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			return trunc(self);
		}
	}

	static Object trunc(Object n) {
		if (isInt(n) || n instanceof BigInteger)
			return n;
		if (n instanceof BigDecimal)
			return ((BigDecimal) n).toBigInteger();
		else
			return (long) ((Number) n).doubleValue();
	}

	public static class Hex extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			long n = ((Number) self).longValue();
			return Long.toHexString(n);
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

	public static class Pow extends SuMethod1 {
		{ params = new FunctionSpec("number"); }
		@Override
		public Object eval1(Object self, Object a) {
			double d = ((Number) self).doubleValue();
			double e = ((Number) a).doubleValue();
			return Math.pow(d, e);
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
