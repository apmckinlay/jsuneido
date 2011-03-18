/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.*;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.annotation.concurrent.ThreadSafe;

import suneido.*;
import suneido.language.builtin.*;
import suneido.util.StringIterator;

import com.google.common.base.Splitter;

@ThreadSafe // all static methods
public final class Ops {

	public static SuException BREAK_EXCEPTION = new SuException("block:break");
	public static SuException CONTINUE_EXCEPTION = new SuException("block:continue");

	public static boolean is_(Object x, Object y) {
		if (x == y)
			return true;
		if (x == null || y == null)
			return false;
		/* NOTE: cannot compare hashCode's for inequality
		 * because Suneido can compare different types as equal
		 * for example String and Concat or Integer and BigDecimal */

		Class<?> xClass = x.getClass();
		Class<?> yClass = y.getClass();

		// have to use compareTo for BigDecimal
		if (xClass == yClass && xClass != BigDecimal.class)
			return x.equals(y);

		if (x instanceof Number && y instanceof Number) {
			if ((xClass == Integer.class || xClass == Long.class ||
					xClass == Byte.class || xClass == Short.class) &&
					(yClass == Integer.class || yClass == Long.class ||
					yClass == Byte.class || yClass == Short.class))
				return ((Number) x).longValue() == ((Number) y).longValue();
			else if ((xClass == Float.class || xClass == Double.class) &&
					(yClass == Float.class || yClass == Double.class))
				return ((Number) x).doubleValue() == ((Number) y).doubleValue();
			else
				return 0 == new BigDecimal(x.toString()).compareTo(
						new BigDecimal(y.toString()));
		}

		if (y instanceof Concat)
			return y.equals(x);

		return x.equals(y);
	}

	public static Boolean is(Object x, Object y) {
		return is_(x, y);
	}
	public static Boolean isnt(Object x, Object y) {
		return !is_(x, y);
	}

	public static Boolean lt(Object x, Object y) {
		return cmp(x, y) < 0;
	}
	public static Boolean lte(Object x, Object y) {
		return cmp(x, y) <= 0;
	}
	public static Boolean gt(Object x, Object y) {
		return cmp(x, y) > 0;
	}
	public static Boolean gte(Object x, Object y) {
		return cmp(x, y) >= 0;
	}

	public static boolean isnt_(Object x, Object y) {
		return !is_(x, y);
	}
	public static boolean lt_(Object x, Object y) {
		return cmp(x, y) < 0;
	}
	public static boolean lte_(Object x, Object y) {
		return cmp(x, y) <= 0;
	}
	public static boolean gt_(Object x, Object y) {
		return cmp(x, y) > 0;
	}
	public static boolean gte_(Object x, Object y) {
		return cmp(x, y) >= 0;
	}

	/**
	 * type ordering: boolean, number, string, date, container, other
	 */
	@SuppressWarnings("unchecked")
	public static int cmp(Object x, Object y) {
		if (x == y)
			return 0;

		Class<?> xClass = x.getClass();
		Class<?> yClass = y.getClass();

		if (xClass == SuRecord.class)
			xClass = SuContainer.class;
		if (yClass == SuRecord.class)
			yClass = SuContainer.class;
		if (xClass == Concat.class) {
			x = x.toString();
			xClass = String.class;
		}
		if (yClass == Concat.class) {
			y = y.toString();
			yClass = String.class;
		}
		if (xClass == yClass)
			return (x instanceof Comparable)
				? ((Comparable<Object>) x).compareTo(y)
				: cmpHash(xClass, yClass);

		if (x instanceof Number && y instanceof Number) {
			if ((xClass == Integer.class || xClass == Long.class ||
					xClass == Byte.class || xClass == Short.class) &&
					(yClass == Integer.class || yClass == Long.class ||
					yClass == Byte.class || yClass == Short.class)) {
				long x1 = ((Number) x).longValue();
				long y1 = ((Number) y).longValue();
				return x1 < y1 ? -1 : x1 > y1 ? +1 : 0;
			} else if ((xClass == Float.class || xClass == Double.class) &&
					(yClass == Float.class || yClass == Double.class)) {
				double x1 = ((Number) x).doubleValue();
				double y1 = ((Number) y).doubleValue();
				return x1 < y1 ? -1 : x1 > y1 ? +1 : 0;
			} else
				return new BigDecimal(x.toString()).compareTo(
						new BigDecimal(y.toString()));
		}

		if (xClass == Boolean.class)
			return -1;
		if (yClass == Boolean.class)
			return +1;

		if (x instanceof Number)
			return -1;
		if (y instanceof Number)
			return +1;

		if (xClass == String.class)
			return -1;
		if (yClass == String.class)
			return +1;

		if (xClass == Date.class)
			return -1;
		if (yClass == Date.class)
			return +1;

		if (xClass == SuContainer.class)
			return -1;
		if (yClass == SuContainer.class)
			return +1;

		return cmpHash(xClass, yClass);
	}

	private static int cmpHash(Class<?> xType, Class<?> yType) {
		int xHash = xType.hashCode();
		int yHash = yType.hashCode();
		return xHash < yHash ? -1 : xHash > yHash ? +1 : 0;
	}

	public static final class Comp implements Comparator<Object> {
		public int compare(Object x, Object y) {
			return cmp(x, y);
		}
	}
	public static final Comp comp = new Comp();

	public static boolean match_(Object s, Object rx) {
		return Regex.contains(toStr(s), toStr(rx));
	}
	public static Boolean match(Object s, Object rx) {
		return match_(s, rx);
	}
	public static Boolean matchnot(Object s, Object rx) {
		return ! match_(s, rx);
	}
	public static boolean matchnot_(Object s, Object rx) {
		return ! match_(s, rx);
	}

	public static Object cat(Object x, Object y) {
		if (x instanceof String && y instanceof String)
			return cat((String) x, (String) y);
		return cat2(x, y);
	}

	private static final int LARGE = 256;

	private static Object cat(String x, String y) {
		int n = x.length() + y.length();
		return n < LARGE
				? x.concat(y)
				: new Concat(x, y, n);
	}

	private static Object cat2(Object x, Object y) {
		if (x instanceof Concat)
			if (y instanceof Concat)
				return new Concat(x, y);
			else
				return new Concat(x, toStr(y));
		else if (y instanceof Concat)
			return new Concat(toStr(x), y);
		return cat(toStr(x), toStr(y));
	}

	public static boolean isString(Object x) {
		return x instanceof String || x instanceof Concat;
	}

	public final static int PRECISION = 16; // to match cSuneido
	public final static MathContext MC = new MathContext(PRECISION);

	// fast path, kept small in hopes of getting inlined
	public static Number add(Object x, Object y) {
		if (x instanceof Integer && y instanceof Integer)
			return (Integer) x + (Integer) y;
		return add2(x, y);
	}

	// slow path
	private static Number add2(Object x, Object y) {
		x = toNum(x);
		y = toNum(y);

		boolean xIsInt = x instanceof Integer || x instanceof Long ||
				x instanceof Short || x instanceof Byte;
		boolean yIsInt = y instanceof Integer || y instanceof Long ||
				y instanceof Short || y instanceof Byte;
		if (xIsInt && yIsInt)
			return narrow(((Number) x).longValue() + ((Number) y).longValue());
		if ((xIsInt || x instanceof Float || x instanceof Double) &&
				(yIsInt || y instanceof Float || y instanceof Double))
			return ((Number) x).doubleValue() + ((Number) y).doubleValue();

		if (x == INF)
			return y == MINUS_INF ? 0 : INF;
		if (y == INF)
			return x == MINUS_INF ? 0 : INF;
		if (x == MINUS_INF)
			return y == INF ? 0 : MINUS_INF;
		if (y == MINUS_INF)
			return x == INF ? 0 : MINUS_INF;

		return toBigDecimal(x).add(toBigDecimal(y), MC);
	}

	private static Number toNum(Object x) {
		if (x instanceof Number)
			return (Number) x;
		if (x instanceof String || x instanceof Concat)
			return stringToPlainNumber(x.toString());
		if (x instanceof Boolean)
			return (Boolean) x ? 1 : 0;
		throw new SuException("can't convert " + typeName(x) + " to number");
	}

	private static Number narrow(long x) {
		if (Integer.MIN_VALUE <= x && x <= Integer.MAX_VALUE)
			return (int) x;
		else
			return x;
		// NOTE: can't use ?: because it would convert to same type
	}

	public static BigDecimal toBigDecimal(Object n) {
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

	// fast path, kept small in hopes of getting inlined
	public static Number sub(Object x, Object y) {
		if (x instanceof Integer && y instanceof Integer)
			return (Integer) x - (Integer) y;
		return sub2(x, y);
	}

	// slow path
	private static Number sub2(Object x, Object y) {
		x = toNum(x);
		y = toNum(y);

		boolean xIsInt = x instanceof Integer || x instanceof Long ||
				x instanceof Short || x instanceof Byte;
		boolean yIsInt = y instanceof Integer || y instanceof Long ||
				y instanceof Short || y instanceof Byte;
		if (xIsInt && yIsInt)
			return narrow(((Number) x).longValue() - ((Number) y).longValue());
		if ((xIsInt || x instanceof Float || x instanceof Double) &&
				(yIsInt || y instanceof Float || y instanceof Double))
			return ((Number) x).doubleValue() - ((Number) y).doubleValue();

		if (x == INF)
			return y == INF ? 0 : INF;
		if (y == INF)
			return x == INF ? 0 : MINUS_INF;
		if (x == MINUS_INF)
			return y == MINUS_INF ? 0 : MINUS_INF;
		if (y == MINUS_INF)
			return x == MINUS_INF ? 0 : INF;

		return toBigDecimal(x).subtract(toBigDecimal(y), MC);
	}

	private static final Integer one = 1;
	public static Number add1(Object x) {
		return add(x, one);
	}
	public static Number sub1(Object x) {
		return sub(x, one);
	}

	// fast path, kept small in hopes of getting inlined
	public static Number mul(Object x, Object y) {
		if (x instanceof Integer && y instanceof Integer)
			return (Integer) x * (Integer) y;
		return mul2(x, y);
	}

	// slow path
	private static Number mul2(Object x, Object y) {
		x = toNum(x);
		y = toNum(y);

		boolean xIsInt = x instanceof Integer || x instanceof Long ||
				x instanceof Short || x instanceof Byte;
		boolean yIsInt = y instanceof Integer || y instanceof Long ||
				y instanceof Short || y instanceof Byte;
		if (xIsInt && yIsInt)
			return narrow(((Number) x).longValue() * ((Number) y).longValue());
		if ((xIsInt || x instanceof Float || x instanceof Double) &&
				(yIsInt || y instanceof Float || y instanceof Double))
			return ((Number) x).doubleValue() * ((Number) y).doubleValue();

		BigDecimal xbd = toBigDecimal(x);
		BigDecimal ybd = toBigDecimal(y);

		if (xbd.signum() == 0 || ybd.signum() == 0)
			return 0;
		if (x == INF)
			return (ybd.signum() < 0) ? MINUS_INF : INF;
		if (y == INF)
			return (xbd.signum() < 0) ? MINUS_INF : INF;
		if (x == MINUS_INF)
			return (ybd.signum() < 0) ? INF : MINUS_INF;
		if (y == MINUS_INF)
			return (xbd.signum() < 0) ? INF : MINUS_INF;

		return xbd.multiply(ybd, MC);
	}

	public final static BigDecimal ZERO = BigDecimal.ZERO;
	public final static BigDecimal INF =
			BigDecimal.valueOf(1, -4 * Byte.MAX_VALUE);
	public final static BigDecimal MINUS_INF =
			BigDecimal.valueOf(-1, -4 * Byte.MAX_VALUE);

	public static Number div(Object x, Object y) {
		x = toNum(x);
		y = toNum(y);

		boolean xIsInt = x instanceof Integer || x instanceof Long ||
				x instanceof Short || x instanceof Byte;
		boolean yIsInt = y instanceof Integer || y instanceof Long ||
				y instanceof Short || y instanceof Byte;
		if (! (xIsInt && yIsInt) &&
				(x instanceof Float || x instanceof Double || xIsInt) &&
				(y instanceof Float || y instanceof Double || yIsInt))
			return ((Number) x).doubleValue() / ((Number) y).doubleValue();

		BigDecimal xbd = toBigDecimal(x);
		BigDecimal ybd = toBigDecimal(y);

		if (xbd.signum() == 0)
			return 0;
		if (x == INF)
			return y == INF ? +1 : y == MINUS_INF ? -1
					: (ybd.signum() < 0) ? MINUS_INF : INF;
		if (x == MINUS_INF)
			return y == INF ? -1 : y == MINUS_INF ? +1
					: (ybd.signum() < 0) ? INF : MINUS_INF;
		if (y == INF || y == MINUS_INF)
			return 0;
		if (ybd.signum() == 0)
			return xbd.signum() < 0 ? MINUS_INF : INF;

		return xbd.divide(ybd, MC);
	}

	public static Number mod(Object x, Object y) {
		return toInt(x) % toInt(y);
	}

	public static Number uminus(Object x) {
		x = toNum(x);
		if (x instanceof Integer)
			return -(Integer) x;
		if (x instanceof BigDecimal)
			return ((BigDecimal) x).negate();
		if (x instanceof BigInteger)
			return ((BigInteger) x).negate();
		if (x instanceof Long)
			return -(Long) x;
		if (x instanceof Short)
			return -(Short) x;
		if (x instanceof Byte)
			return -(Byte) x;
		if (x instanceof Float)
			return -(Float) x;
		if (x instanceof Double)
			return -(Double) x;
		throw SuException.unreachable();
	}

	public static boolean not_(Object x) {
		if (x == Boolean.TRUE)
			return false;
		if (x == Boolean.FALSE)
			return true;
		throw new SuException("can't do: not " + typeName(x));
	}

	public static Boolean not(Object x) {
		return not_(x);
	}

	public static Integer bitnot(Object x) {
		return ~toInt(x);
	}
	public static Integer bitand(Object x, Object y) {
		return toInt(x) & toInt(y);
	}
	public static Integer bitor(Object x, Object y) {
		return toInt(x) | toInt(y);
	}
	public static Integer bitxor(Object x, Object y) {
		return toInt(x) ^ toInt(y);
	}
	public static Integer lshift(Object x, Object y) {
		return toInt(x) << toInt(y);
	}
	public static Integer rshift(Object x, Object y) {
		return toInt(x) >> toInt(y);
	}

	public static Number stringToNumber(String s) {
		if (s.startsWith("0x"))
			return (int) Long.parseLong(s.substring(2), 16);
		if (s.startsWith("0") && s.indexOf('.') == -1)
			return (int) Long.parseLong(s, 8);
		return stringToPlainNumber(s);
	}

	public static Number stringToPlainNumber(String s) {
		if (s.length() == 0)
			return 0;
		else if (s.indexOf('.') == -1 && s.indexOf('e') == -1
				&& s.indexOf("E") == -1 && s.length() < 10)
			return Integer.parseInt(s);
		else
			try {
				BigDecimal n = new BigDecimal(s, MC);
				if (n.compareTo(BigDecimal.ZERO) == 0)
					return 0;
				return n;
			} catch (NumberFormatException e) {
				throw new SuException("can't convert to number: " + s, e);
			}
	}

	public static Date stringToDate(String s) {
		if (s.startsWith("#"))
			s = s.substring(1);
		if (s.length() < 8 || 18 < s.length())
			return null;
		if (s.length() == 8)
			s += ".";
		if (s.length() < 18)
			s = (s + "000000000").substring(0, 18);
		ParsePosition pos = new ParsePosition(0);
		Date date = new SimpleDateFormat("yyyyMMdd.HHmmssSSS").parse(s, pos);
		if (date == null || pos.getIndex() != 18)
			return null;
		return date;
	}

	public static int toIntBool(Object x) {
		if (x == Boolean.TRUE)
			return 1;
		if (x == Boolean.FALSE)
			return 0;
		throw new SuException("conditionals require true or false, got: "
				+ typeName(x));
	}

	public static Boolean toBoolean(Object x) {
		return toBoolean_(x);
	}

	public static Boolean toBoolean_(Object x) {
		if (x == Boolean.TRUE)
			return true;
		else if (x == Boolean.FALSE)
			return false;
		throw new SuException("can't convert " + typeName(x) + " to boolean");
	}

	public static int toInt(Object x) {
		if (x instanceof Integer || x instanceof Long ||
				x instanceof Short || x instanceof Byte)
			return ((Number) x).intValue();
		if (x instanceof Long)
			return toIntLong((Long) x);
		if (x instanceof BigDecimal)
			return toIntBD((BigDecimal) x);
		if (x instanceof BigInteger)
			return toIntBI((BigInteger) x);
		if (x instanceof String || x instanceof Concat)
			return toIntS(x.toString());
		if (x instanceof Boolean)
			return x == Boolean.TRUE ? 1 : 0;
		throw new SuException("can't convert " + typeName(x) + " to integer");
	}

	public static int toIntLong(long n) {
		if (n < Integer.MIN_VALUE)
			return Integer.MIN_VALUE;
		if (n > Integer.MAX_VALUE)
			return Integer.MAX_VALUE;
		return (int) n;
	}

	public final static BigDecimal BD_INT_MIN = BigDecimal.valueOf(Integer.MIN_VALUE);
	public final static BigDecimal BD_INT_MAX = BigDecimal.valueOf(Integer.MAX_VALUE);

	public static int toIntBD(BigDecimal n) {
		if (n.compareTo(BD_INT_MIN) == -1)
			return Integer.MIN_VALUE;
		if (n.compareTo(BD_INT_MAX) == 1)
			return Integer.MAX_VALUE;
		return n.intValue();
	}

	public final static BigInteger BI_INT_MIN = BigInteger.valueOf(Integer.MIN_VALUE);
	public final static BigInteger BI_INT_MAX = BigInteger.valueOf(Integer.MAX_VALUE);

	public static int toIntBI(BigInteger n) {
		if (n.compareTo(BI_INT_MIN) == -1)
			return Integer.MIN_VALUE;
		if (n.compareTo(BI_INT_MAX) == 1)
			return Integer.MAX_VALUE;
		return n.intValue();
	}

	public static int toIntS(String s) {
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

	public static String toStr(Object x) {
		if (x instanceof BigDecimal)
			return toStringBD((BigDecimal) x);
		if (x instanceof Date)
			return toStringDate((Date) x);
		return x.toString();
	}

	private static String toStringDate(Date x) {
		String s = "#" + new SimpleDateFormat("yyyyMMdd.HHmmssSSS").format(x);
		if (s.endsWith("000000000"))
			return s.substring(0, 9);
		if (s.endsWith("00000"))
			return s.substring(0, 14);
		if (s.endsWith("000"))
			return s.substring(0, 16);
		return s;
	}

	public static boolean default_single_quotes = false;

	public static String display(Object x) {
		if (isString(x)) {
			String s = x.toString();
			s = s.replace("\\", "\\\\");
			boolean single_quotes = default_single_quotes
				? !s.contains("'")
				: (s.contains("\"") && !s.contains("'"));
			if (single_quotes)
				return "'" + s + "'";
			else
				return "\"" + s.replace("\"", "\\\"") + "\"";
		}
		if (x == null)
			return "null";
		return toStr(x);
	}
	public static String display(Object[] a) {
		if (a.length == 0)
			return "()";
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (Object x : a)
			sb.append(display(x) + ", ");
		sb.delete(sb.length() - 2, sb.length());
		sb.append(")");
		return sb.toString();
	}

	public static String toStringBD(BigDecimal n) {
		if (n.compareTo(INF) == 0)
			return "Inf";
		if (n.compareTo(MINUS_INF) == 0)
			return "-Inf";
		n = n.stripTrailingZeros();
		String s = Math.abs(n.scale()) >= 20 ? n.toString() : n.toPlainString();
		return removeLeadingZero(s).replace("E", "e").replace("e+", "e");
	}
	private static String removeLeadingZero(String s) {
		if (s.startsWith("0.") && s.length() > 2)
			s = s.substring(1);
		return s;
	}

	public static SuContainer toContainer(Object x) {
		return x instanceof SuValue ? ((SuValue) x).toContainer() : null;
	}

	public static String typeName(Object x) {
		if (x == null)
			return "uninitialized";
		if (x instanceof SuValue)
			return ((SuValue) x).typeName();
		Class<?> xType = x.getClass();
		if (xType == String.class)
			return "String";
		if (xType == Boolean.class)
			return "Boolean";
		if (xType == Date.class)
			return "Date";
		if (xType == Integer.class || xType == Long.class ||
				xType == BigDecimal.class)
			return "Number";
		return x.getClass().getName();
	}

	public static SuException exception(Object e) {
		return new SuException(e);
	}
	public static void throwUninitializedVariable() {
		throw new SuException("uninitialized variable");
	}
	public static void throwNoReturnValue() {
		throw new SuException("no return value");
	}
	public static void throwNoValue() {
		throw new SuException("no value");
	}

	// so far only SuValue and String are callable
	// so don't need to use target like invoke does
	public static Object call(Object x, Object... args) {
		if (x instanceof SuValue)
			return ((SuValue) x).call(args);
		if (x instanceof String)
			return callString(x, args);
		throw new SuException("can't call " + typeName(x) + " (" + x + ")");
	}

	public static Object call0(Object x) {
		return (x instanceof SuValue)
			? ((SuValue) x).call0()
			: call(x);
	}
	public static Object call1(Object x, Object a) {
		return (x instanceof SuValue)
			? ((SuValue) x).call1(a)
			: call(x, a);
	}
	public static Object call2(Object x, Object a, Object b) {
		return (x instanceof SuValue)
			? ((SuValue) x).call2(a, b)
			: call(x, a, b);
	}
	public static Object call3(Object x, Object a, Object b, Object c) {
		return (x instanceof SuValue)
			? ((SuValue) x).call3(a, b, c)
			: call(x, a, b, c);
	}
	public static Object call4(Object x, Object a, Object b, Object c, Object d) {
		return (x instanceof SuValue)
			? ((SuValue) x).call4(a, b, c, d)
			: call(x, a, b, c, d);
	}

	/** string(object, ...) => object[string](...) */
	static Object callString(Object x, Object... args) {
		Object ob = args[0];
		args = Arrays.copyOfRange(args, 1, args.length);
		return invoke(ob, x.toString(), args);
	}

	/** Used by generated code to call methods */
	public static Object invoke(Object x, String method, Object... args) {
		return target(x).lookup(method).eval(x, args);
	}

	public static Object invoke0(Object x, String method) {
		return target(x).lookup(method).eval0(x);
	}
	public static Object invoke1(Object x, String method, Object a) {
		return target(x).lookup(method).eval1(x, a);
	}
	public static Object invoke2(Object x, String method, Object a, Object b) {
		return target(x).lookup(method).eval2(x, a, b);
	}
	public static Object invoke3(Object x, String method, Object a, Object b,
			Object c) {
		return target(x).lookup(method).eval3(x, a, b, c);
	}
	public static Object invoke4(Object x, String method, Object a, Object b,
			Object c, Object d) {
		return target(x).lookup(method).eval4(x, a, b, c, d);
	}

	public static SuValue target(Object x) {
		if (x instanceof SuValue)
			return (SuValue) x;
		if (x instanceof String)
			return StringMethods.singleton;
		if (x instanceof Number) // e.g. Integer, Float, BigDecimal
			return NumberMethods.singleton;
		if (x instanceof Date)
			return DateMethods.singleton;
		return invokeUnknown;
	}

	private static SuValue invokeUnknown = new SuValue() { };

	public static String toMethodString(Object method) {
		if (isString(method))
			return method.toString().intern();
		throw new SuException("invalid method: " + method);
	}

	// TODO change get and put to use target

	public static void put(Object x, Object member, Object value) {
		if (x instanceof SuValue)
			((SuValue) x).put(member, value);
		else
			throw new SuException(typeName(x) + " does not support put");
	}

	public static Object get(Object x, Object member) {
		if (x == null || member == null)
			throw new SuException("uninitialized");
		if (x instanceof SuValue) {
			Object y = ((SuValue) x).get(member);
			if (y == null)
				throw new SuException("uninitialized member " + member);
			return y;
		} else if (isString(x))
			return getString(x.toString(), toInt(member));
		else if (x instanceof Object[])
			return getArray((Object[]) x, toInt(member));
		else if (x instanceof Boolean || x instanceof Number)
			; // fall thru to error
		else if (isString(member))
			return getProperty(x, member.toString());
		throw new SuException(typeName(x) + " does not support get " + x + "." + member);
	}

	private static Object getProperty(Object x, String member) {
		try {
			Method m = x.getClass().getMethod("get" + capitalized(member));
			return m.invoke(x);
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		}
		throw new SuException("get property failed: " + x + "." + member);
	}
	private static String capitalized(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	private static Object getArray(Object[] x, int i) {
		return x[i];
	}

	private static Object getString(String s, int i) {
		return 0 > i || i >= s.length() ? "" : s.substring(i, i + 1);
	}

	public static Object iterator(Object x) {
		if (x instanceof Iterable<?>)
			return ((Iterable<?>) x).iterator();
		else if (isString(x))
			return new StringIterator(x.toString());
		else if (x instanceof Object[])
			return Arrays.asList((Object[]) x).iterator();
		throw new SuException("can't iterate " + typeName(x));
	}
	public static boolean hasNext(Object x) {
		if (x instanceof Iterator<?>)
			return ((Iterator<?>) x).hasNext();
		throw new SuException("not an iterator " + typeName(x));
	}
	public static Object next(Object x) {
		if (x instanceof Iterator<?>)
			try {
				return ((Iterator<?>) x).next();
			} catch (ConcurrentModificationException e) {
				throw new SuException("object modified during iteration");
			}
		throw new SuException("not an iterator " + typeName(x));
	}

	private static final Splitter catchSplitter = Splitter.on('|');

	public static Except catchMatch(SuException e) {
		return new Except(e);
	}
	public static Except catchMatch(SuException e, String patterns) {
		if (catchMatch(e.toString(), patterns))
			return new Except(e);
		throw e; // no match so rethrow
	}
	private static boolean catchMatch(String es, String patterns) {
		for (String pat : catchSplitter.split(patterns))
			if (pat.startsWith("*")
					? es.contains(pat.substring(1)) : es.startsWith(pat))
				return true;
		return false;
	}

	public static BlockReturnException blockReturnException(Object returnValue, int parent) {
		return new BlockReturnException(returnValue, parent);
	}

	/**
	 * If block return came from one of our blocks, then return the value,
	 * otherwise, re-throw.
	 */
	public static Object blockReturnHandler(BlockReturnException e, int id) {
		if (id == e.parent)
			return e.returnValue;
		else
			throw e;
	}

}
