package suneido.language;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.*;

import suneido.*;
import suneido.util.StringIterator;

public class Ops {

	public static boolean is_(Object x, Object y) {
		if (x == y)
			return true;
		if (x == null || y == null)
			return false;
		Class<?> xType = x.getClass();
		if (xType == Integer.class) {
			if (y.getClass() == BigDecimal.class)
				x = BigDecimal.valueOf((Integer) x);
		} else if (xType == BigDecimal.class) {
			if (y.getClass() == Integer.class)
				y = BigDecimal.valueOf((Integer) y);
		}
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
	public static int cmp(Object x, Object y) {
		if (x == y)
			return 0;
		Class<?> xType = x.getClass();
		Class<?> yType = y.getClass();
		if (xType == yType) {
			if (xType == Integer.class)
				return ((Integer) x).compareTo((Integer) y);
			if (xType == String.class)
				return ((String) x).compareTo((String) y);
			if (xType == Boolean.class)
				return ((Boolean) x).compareTo((Boolean) y);
			if (xType == BigDecimal.class)
				return ((BigDecimal) x).compareTo((BigDecimal) y);
			if (xType == Date.class)
				return ((Date) x).compareTo((Date) y);
			if (xType == SuContainer.class)
				return ((SuContainer) x).compareTo((SuContainer) y);
			int xHash = x.hashCode();
			int yHash = y.hashCode();
			return xHash < yHash ? -1 : xHash > yHash ? +1 : 0;
		}
		if (xType == Boolean.class)
			return -1;
		if (xType == Integer.class) {
			if (yType == Boolean.class)
				return +1;
			if (yType == BigDecimal.class)
				return BigDecimal.valueOf((Integer) x).compareTo((BigDecimal) y);
			return -1;
		}
		if (xType == BigDecimal.class) {
			if (yType == Boolean.class)
				return +1;
			if (yType == Integer.class)
				return ((BigDecimal) x).compareTo(BigDecimal.valueOf((Integer) y));
			return -1;
		}
		if (xType == String.class)
			return yType == Boolean.class || yType == Integer.class
					|| yType == BigDecimal.class ? +1 : -1;
		if (xType == Date.class)
			return yType == Boolean.class || yType == Integer.class
					|| yType == BigDecimal.class || yType == String.class ? +1
					: -1;
		if (x instanceof SuContainer)
			return yType == Boolean.class || yType == Integer.class
				|| yType == BigDecimal.class || yType == String.class
				|| yType == Date.class
				? +1 : -1;

		if (yType == Boolean.class || yType == Integer.class
				|| yType == BigDecimal.class || yType == String.class)
			return +1;
		int xHash = xType.hashCode();
		int yHash = yType.hashCode();
		return xHash < yHash ? -1 : xHash > yHash ? +1 : 0;
	}

	// TODO convert from Suneido regex and cache compiled patterns
	public static boolean match_(Object s, Object rx) {
		if (s instanceof String && rx instanceof String)
			return Regex.contains((String) s, (String) rx);
		throw cant(s, " =~ ", rx);
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

	public static String cat(Object x, Object y) {
		return toStr(x).concat(toStr(y));
	}

	private static SuException cant(Object x, String op, Object y) {
		return new SuException("can't do " + typeName(x) + op + typeName(y));
	}

	public static Number add(Object x, Object y) {
		Class<?> xType = x.getClass();
		if (xType == String.class) {
			x = stringToNumber((String) x);
			xType = x.getClass();
		}
		Class<?> yType = y.getClass();
		if (yType == String.class) {
			y = stringToNumber((String) y);
			yType = y.getClass();
		}
		if (xType == Integer.class) {
			if (yType == Integer.class)
				return (Integer) x + (Integer) y;
			if (yType == BigDecimal.class)
				return BigDecimal.valueOf((Integer) x).add((BigDecimal) y);
		} else if (xType == BigDecimal.class) {
			if (yType == BigDecimal.class)
				return ((BigDecimal) x).add((BigDecimal) y);
			else if (yType == Integer.class)
				return ((BigDecimal) x).add(BigDecimal.valueOf((Integer) y));
		}
		throw cant(x, " + ", y);
	}

	public static Number sub(Object x, Object y) {
		Class<?> xType = x.getClass();
		if (xType == String.class) {
			x = stringToNumber((String) x);
			xType = x.getClass();
		}
		Class<?> yType = y.getClass();
		if (yType == String.class) {
			y = stringToNumber((String) y);
			yType = y.getClass();
		}
		if (xType == Integer.class) {
			if (yType == Integer.class)
				return (Integer) x - (Integer) y;
			if (yType == BigDecimal.class)
				return BigDecimal.valueOf((Integer) x).subtract((BigDecimal) y);
		} else if (xType == BigDecimal.class) {
			if (yType == BigDecimal.class)
				return ((BigDecimal) x).subtract((BigDecimal) y);
			else if (yType == Integer.class)
				return ((BigDecimal) x).subtract(BigDecimal.valueOf((Integer) y));
		}
		throw cant(x, " - ", y);
	}

	private static Integer one = 1;
	public static Number add1(Object x) {
		return add(x, one);
	}
	public static Number sub1(Object x) {
		return sub(x, one);
	}

	public static Number mul(Object x, Object y) {
		Class<?> xType = x.getClass();
		if (xType == String.class) {
			x = stringToNumber((String) x);
			xType = x.getClass();
		}
		Class<?> yType = y.getClass();
		if (yType == String.class) {
			y = stringToNumber((String) y);
			yType = y.getClass();
		}
		if (xType == Integer.class) {
			if (yType == Integer.class)
				return (Integer) x * (Integer) y;
			if (yType == BigDecimal.class)
				return BigDecimal.valueOf((Integer) x).multiply((BigDecimal) y);
		} else if (xType == BigDecimal.class) {
			if (yType == BigDecimal.class)
				return ((BigDecimal) x).multiply((BigDecimal) y);
			else if (yType == Integer.class)
				return ((BigDecimal) x).multiply(BigDecimal.valueOf((Integer) y));
		}
		throw cant(x, " * ", y);
	}

	public final static MathContext mc = new MathContext(16);

	public static Number div(Object x, Object y) {
		Class<?> xType = x.getClass();
		if (xType == String.class) {
			x = stringToNumber((String) x);
			xType = x.getClass();
		}
		Class<?> yType = y.getClass();
		if (yType == String.class) {
			y = stringToNumber((String) y);
			yType = y.getClass();
		}
		if (xType == Integer.class) {
			if (yType == Integer.class)
				return BigDecimal.valueOf((Integer) x).divide(
						BigDecimal.valueOf((Integer) y), mc);
			if (yType == BigDecimal.class)
				return BigDecimal.valueOf((Integer) x).divide((BigDecimal) y,
						mc);
		} else if (xType == BigDecimal.class) {
			if (yType == BigDecimal.class)
				return ((BigDecimal) x).divide((BigDecimal) y, mc);
			else if (yType == Integer.class)
				return ((BigDecimal) x).divide(BigDecimal.valueOf((Integer) y),
						mc);
		}
		throw cant(x, " / ", y);
	}

	public static Number mod(Object x, Object y) {
		return toInt(x) % toInt(y);
	}

	public static Number uminus(Object x) {
		Class<?> xType = x.getClass();
		if (xType == String.class) {
			x = stringToNumber((String) x);
			xType = x.getClass();
		}
		if (xType == Integer.class)
			return -(Integer) x;
		else if (xType == BigDecimal.class)
			return ((BigDecimal) x).negate();
		else
			throw new SuException("can't do - " + typeName(x));
	}

	public static Boolean not(Object x) {
		if (x == Boolean.TRUE)
			return Boolean.FALSE;
		if (x == Boolean.FALSE)
			return Boolean.TRUE;
		throw new SuException("can't do not " + typeName(x));
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
			return Integer.parseInt(s.substring(2), 16);
		else if (s.startsWith("0"))
			return Integer.parseInt(s, 8);
		else if (s.indexOf('.') == -1 && s.indexOf('e') == -1
				&& s.indexOf("E") == -1 && s.length() < 10)
			return Integer.parseInt(s);
		else
			return new BigDecimal(s);
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

	public static int toBool(Object x) {
		if (x == Boolean.TRUE)
			return 1;
		if (x == Boolean.FALSE)
			return 0;
		throw new SuException("conditionals require true or false, got: "
				+ typeName(x));
	}

	public static int toInt(Object x) {
		Class<?> xType = x.getClass();
		if (xType == Integer.class)
			return ((Integer) x);
		else if (xType == BigDecimal.class)
			return toIntBD((BigDecimal) x);
		else if (xType == String.class)
			return toIntS((String) x);
		else
			throw new SuException("can't convert " + typeName(x)
					+ " to integer");
	}

	public final static BigDecimal INT_MIN = new BigDecimal(Integer.MIN_VALUE);
	public final static BigDecimal INT_MAX = new BigDecimal(Integer.MAX_VALUE);

	public static int toIntBD(BigDecimal n) {
		if (n.compareTo(INT_MIN) == -1)
			return Integer.MIN_VALUE;
		else if (n.compareTo(INT_MAX) == 1)
			return Integer.MAX_VALUE;
		else
			return n.intValue();
	}

	public static int toIntS(String s) {
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
			throw new SuException("can't convert string to integer");
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

	public static String display(Object x) {
		if (x instanceof String)
			return "'" + ((String) x).replace("'", "\\'") + "'"; //TODO smarter quoting/escaping
		else
			return toStr(x);
	}

	public static String toStringBD(BigDecimal n) {
		n = n.stripTrailingZeros();
		String s = Math.abs(n.scale()) > 10 ? n.toString() : n.toPlainString();
		return removeLeadingZero(s).replace("E", "e").replace("e+", "e");
	}
	private static String removeLeadingZero(String s) {
		if (s.startsWith("0.") && s.length() > 2)
			s = s.substring(1);
		return s;
	}

	public static int hashCode(Object x, int nest) {
		if (x instanceof SuContainer)
			return ((SuContainer) x).hashCode(nest);
		else if (x instanceof BigDecimal)
			return canonical(x).hashCode();
		else
			return x.hashCode();
	}

	public static Object canonical(Object x) {
		return x instanceof BigDecimal ? canonicalBD((BigDecimal) x) : x;
	}
	public static Number canonicalBD(BigDecimal n) {
		try {
			return n.intValueExact();
		} catch (ArithmeticException e) {
			return n.stripTrailingZeros();
		}
	}

	public static String typeName(Object x) {
		return x == null ? "uninitialized"
				: x.getClass().getName().replaceFirst(
						"^(suneido.(language.)?)?", "");
	}

	public static Object call(Object x, Object... args) {
		if (x instanceof SuValue)
			return ((SuValue) x).call(args);
		throw new SuException("can't call " + typeName(x));
	}

	public static Object invoke(Object x, String method, Object... args) {
		if (x instanceof SuValue)
			return ((SuValue) x).invoke(x, method, args);
		Class<?> xType = x.getClass();
		if (xType == String.class)
			return StringMethods.invoke((String) x, method, args);
		// TODO handle invoke on other types e.g. numbers
		throw new SuException("no such method: " + typeName(x) + method);
	}

	// callN and invokeN are to simplify code generation
	// so caller doesn't have to build args array

	public static Object callN(Object it) {
		return call(it);
	}
	public static Object callN(Object it, Object a) {
		return call(it, a);
	}
	public static Object callN(Object it, Object a, Object b) {
		return call(it, a, b);
	}
	public static Object callN(Object it, Object a, Object b, Object c) {
		return call(it, a, b, c);
	}
	public static Object callN(Object it, Object a, Object b, Object c, Object d) {
		return call(it, a, b, c, d);
	}
	public static Object callN(Object it, Object a, Object b, Object c, Object d, Object e) {
		return call(it, a, b, c, d, e);
	}
	public static Object callN(Object it, Object a, Object b, Object c, Object d, Object e, Object f) {
		return call(it, a, b, c, d, e, f);
	}
	public static Object callN(Object it, Object a, Object b, Object c, Object d, Object e, Object f, Object g) {
		return call(it, a, b, c, d, e, f, g);
	}
	public static Object callN(Object it, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h) {
		return call(it, a, b, c, d, e, f, g, h);
	}
	public static Object callN(Object it, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i) {
		return call(it, a, b, c, d, e, f, g, h, i);
	}
	public static Object callN(Object it, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j) {
		return call(it, a, b, c, d, e, f, g, h, i, j);
	}
	public static Object callN(Object it, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k) {
		return call(it, a, b, c, d, e, f, g, h, i, j, k);
	}
	public static Object callN(Object it, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l) {
		return call(it, a, b, c, d, e, f, g, h, i, j, k, l);
	}
	public static Object callN(Object it, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m) {
		return call(it, a, b, c, d, e, f, g, h, i, j, k, l, m);
	}
	public static Object callN(Object it, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n) {
		return call(it, a, b, c, d, e, f, g, h, i, j, k, l, m, n);
	}
	public static Object callN(Object it, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o) {
		return call(it, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o);
	}
	public static Object callN(Object it, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p) {
		return call(it, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p);
	}
	public static Object callN(Object it, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q) {
		return call(it, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q);
	}
	public static Object callN(Object it, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r) {
		return call(it, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r);
	}
	public static Object callN(Object it, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s) {
		return call(it, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s);
	}
	public static Object callN(Object it, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t) {
		return call(it, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t);
	}
	public static Object callN(Object it, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t, Object u) {
		return call(it, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u);
	}
	public static Object callN(Object it, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t, Object u, Object v) {
		return call(it, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v);
	}
	public static Object callN(Object it, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t, Object u, Object v, Object w) {
		return call(it, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w);
	}
	public static Object callN(Object it, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t, Object u, Object v, Object w, Object x) {
		return call(it, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x);
	}
	public static Object callN(Object it, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t, Object u, Object v, Object w, Object x, Object y) {
		return call(it, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y);
	}
	public static Object callN(Object it, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t, Object u, Object v, Object w, Object x, Object y, Object z) {
		return call(it, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z);
	}

	public static Object invokeN(Object it, String method) {
		return invoke(it, method);
	}
	public static Object invokeN(Object it, String method, Object a) {
		return invoke(it, method, a);
	}
	public static Object invokeN(Object it, String method, Object a, Object b) {
		return invoke(it, method, a, b);
	}
	public static Object invokeN(Object it, String method, Object a, Object b, Object c) {
		return invoke(it, method, a, b, c);
	}
	public static Object invokeN(Object it, String method, Object a, Object b, Object c, Object d) {
		return invoke(it, method, a, b, c, d);
	}
	public static Object invokeN(Object it, String method, Object a, Object b, Object c, Object d, Object e) {
		return invoke(it, method, a, b, c, d, e);
	}
	public static Object invokeN(Object it, String method, Object a, Object b, Object c, Object d, Object e, Object f) {
		return invoke(it, method, a, b, c, d, e, f);
	}
	public static Object invokeN(Object it, String method, Object a, Object b, Object c, Object d, Object e, Object f, Object g) {
		return invoke(it, method, a, b, c, d, e, f, g);
	}
	public static Object invokeN(Object it, String method, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h) {
		return invoke(it, method, a, b, c, d, e, f, g, h);
	}
	public static Object invokeN(Object it, String method, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i) {
		return invoke(it, method, a, b, c, d, e, f, g, h, i);
	}
	public static Object invokeN(Object it, String method, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j) {
		return invoke(it, method, a, b, c, d, e, f, g, h, i, j);
	}
	public static Object invokeN(Object it, String method, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k) {
		return invoke(it, method, a, b, c, d, e, f, g, h, i, j, k);
	}
	public static Object invokeN(Object it, String method, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l) {
		return invoke(it, method, a, b, c, d, e, f, g, h, i, j, k, l);
	}
	public static Object invokeN(Object it, String method, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m) {
		return invoke(it, method, a, b, c, d, e, f, g, h, i, j, k, l, m);
	}
	public static Object invokeN(Object it, String method, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n) {
		return invoke(it, method, a, b, c, d, e, f, g, h, i, j, k, l, m, n);
	}
	public static Object invokeN(Object it, String method, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o) {
		return invoke(it, method, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o);
	}
	public static Object invokeN(Object it, String method, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p) {
		return invoke(it, method, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p);
	}
	public static Object invokeN(Object it, String method, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q) {
		return invoke(it, method, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q);
	}
	public static Object invokeN(Object it, String method, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r) {
		return invoke(it, method, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r);
	}
	public static Object invokeN(Object it, String method, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s) {
		return invoke(it, method, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s);
	}
	public static Object invokeN(Object it, String method, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t) {
		return invoke(it, method, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t);
	}
	public static Object invokeN(Object it, String method, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t, Object u) {
		return invoke(it, method, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u);
	}
	public static Object invokeN(Object it, String method, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t, Object u, Object v) {
		return invoke(it, method, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v);
	}
	public static Object invokeN(Object it, String method, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t, Object u, Object v, Object w) {
		return invoke(it, method, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w);
	}
	public static Object invokeN(Object it, String method, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t, Object u, Object v, Object w, Object x) {
		return invoke(it, method, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x);
	}
	public static Object invokeN(Object it, String method, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t, Object u, Object v, Object w, Object x, Object y) {
		return invoke(it, method, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y);
	}
	public static Object invokeN(Object it, String method, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h, Object i, Object j, Object k, Object l, Object m, Object n, Object o, Object p, Object q, Object r, Object s, Object t, Object u, Object v, Object w, Object x, Object y, Object z) {
		return invoke(it, method, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z);
	}

	public static void put(Object x, Object member, Object value) {
		if (x instanceof SuValue)
			((SuValue) x).put(member, value);
		else
			throw new SuException(typeName(x) + " does not support put");
	}
	public static Object get(Object x, Object member) {
		if (x instanceof SuValue)
			return ((SuValue) x).get(member);
		else if (x instanceof String)
			return getString((String) x, toInt(member));
		else if (x instanceof Object[])
			return getArray((Object[]) x, toInt(member));
		else if (member instanceof String)
			return getProperty(x, (String) member);
		throw new SuException(typeName(x) + " does not support get");
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
		throw new SuException("get property failed");
	}
	private static String capitalized(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	private static Object getArray(Object[] x, int i) {
		return x[i];
	}

	private static Object getString(String s, int i) {
		return 0 > i || i > s.length() ? "" : s.substring(i, i + 1);
	}

	public static Object iterator(Object x) {
		if (x instanceof Iterable<?>)
			return ((Iterable<?>) x).iterator();
		else if (x instanceof String)
			return new StringIterator((String) x);
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
			return ((Iterator<?>) x).next();
		throw new SuException("not an iterator " + typeName(x));
	}

	public static String catchMatch(SuException e, String patterns) {
		String es = e.toString();
		if (patterns == null)
			return es;
		for (String pat : patterns.split("[|]")) {
			if (pat.startsWith("*")) {
				pat = pat.substring(1);
				if (es.contains(pat))
					return es;
			} else if (es.startsWith(pat))
				return es;
		}
		throw e; // no match so rethrow
	}

}
