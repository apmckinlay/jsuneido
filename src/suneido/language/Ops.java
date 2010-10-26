package suneido.language;

import static suneido.SuException.methodNotFound;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.MathContext;
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
		Class<?> xType = x.getClass();
		if (xType == Integer.class) {
			if (y.getClass() == BigDecimal.class) {
				x = BigDecimal.valueOf((Integer) x);
				xType = BigDecimal.class;
			}
		} else if (xType == BigDecimal.class) {
			if (y.getClass() == Integer.class)
				y = BigDecimal.valueOf((Integer) y);
		}
		if (xType == BigDecimal.class && y.getClass() == BigDecimal.class)
			// need to use compareTo to ignore scale
			return 0 == ((BigDecimal) x).compareTo((BigDecimal) y);
		if (y instanceof Concat)
			return x.equals(y.toString());
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
		Class<?> xType = x.getClass();
		if (xType == SuRecord.class)
			xType = SuContainer.class;
		Class<?> yType = y.getClass();
		if (yType == SuRecord.class)
			yType = SuContainer.class;
		if (xType == yType) {
			if (x instanceof Comparable)
				return ((Comparable) x).compareTo(y);
			return cmpHash(xType, yType);
		}
		if (xType == Concat.class) {
			x = x.toString();
			xType = String.class;
		}
		if (yType == Concat.class) {
			y = y.toString();
			yType = String.class;
		}
		if (xType == yType) {
			if (x instanceof Comparable)
				return ((Comparable) x).compareTo(y);
			return cmpHash(xType, yType);
		}
		if (xType == Boolean.class)
			return -1;
		if (xType == Integer.class || xType == Long.class) {
			if (yType == Boolean.class)
				return +1;
			if (yType == BigDecimal.class)
				return BigDecimal.valueOf(((Number) x).longValue()).compareTo((BigDecimal) y);
			return -1;
		}
		if (xType == BigDecimal.class) {
			if (yType == Boolean.class)
				return +1;
			if (yType == Integer.class || yType == Long.class)
				return ((BigDecimal) x).compareTo(BigDecimal.valueOf(((Number) y).longValue()));
			return -1;
		}
		if (xType == String.class)
			return yType == Boolean.class || yType == Integer.class
					|| yType == Long.class || yType == BigDecimal.class ? +1 : -1;
		if (xType == Date.class)
			return yType == Boolean.class || yType == Integer.class
					|| yType == Long.class || yType == BigDecimal.class
					|| yType == String.class ? +1 : -1;
		if (x instanceof SuContainer)
			return yType == Boolean.class || yType == Integer.class
				|| yType == BigDecimal.class || yType == Long.class
				|| yType == String.class || yType == Date.class
				? +1 : -1;

		if (yType == Boolean.class || yType == Integer.class || yType == Long.class
				|| yType == BigDecimal.class || yType == String.class)
			return +1;
		return cmpHash(xType, yType);
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

	public final static MathContext mc = new MathContext(15);
	// must be 15 or less so Pack can multiply by 1000 and still fit in long

	public static Number add(Object x, Object y) {
		if (x instanceof Integer && y instanceof Integer)
			return (Integer) x + (Integer) y;
		return add2(x, y);
	}

	private static Number add2(Object x, Object y) {
		Class<?> xType = x.getClass();
		Class<?> yType = y.getClass();
		if (xType == Integer.class) {
			if (yType == BigDecimal.class) {
				if (y == inf)
					return inf;
				if (y == minus_inf)
					return minus_inf;
				return BigDecimal.valueOf((Integer) x).add((BigDecimal) y, mc);
			}
		} else if (xType == BigDecimal.class) {
			if (yType == Integer.class) {
				if (x == inf)
					return inf;
				if (x == minus_inf)
					return minus_inf;
				return ((BigDecimal) x).add(BigDecimal.valueOf((Integer) y), mc);
			}
			if (yType == BigDecimal.class) {
				if (x == inf)
					return y == minus_inf ? 0 : inf;
				if (x == minus_inf)
					return y == inf ? 0 : minus_inf;
				if (y == inf)
					return inf;
				if (y == minus_inf)
					return minus_inf;
				return ((BigDecimal) x).add((BigDecimal) y, mc);
			}
		}
		return add(toNum(x), toNum(y));
	}

	// used by math ops
	private static Object toNum(Object x) {
		Class<?> xType = x.getClass();
		if (xType == Integer.class || xType == BigDecimal.class)
			return x;
		if (xType == String.class || xType == Concat.class)
			return stringToPlainNumber(x.toString());
		if (xType == Boolean.class)
			return (Boolean) x ? 1 : 0;
		if (xType == Long.class)
			return BigDecimal.valueOf((Long) x);
		// MAYBE other types e.g. long, BigInteger or double
		throw new SuException("can't convert " + typeName(x) + " to number");
	}

	public static Number sub(Object x, Object y) {
		if (x instanceof Integer && y instanceof Integer)
			return (Integer) x - (Integer) y;
		return sub2(x, y);
	}

	private static Number sub2(Object x, Object y) {
		Class<?> xType = x.getClass();
		Class<?> yType = y.getClass();
		if (xType == Integer.class) {
			if (yType == BigDecimal.class) {
				if (y == inf)
					return minus_inf;
				if (y == minus_inf)
					return inf;
				return BigDecimal.valueOf((Integer) x).subtract((BigDecimal) y, mc);
			}
		} else if (xType == BigDecimal.class) {
			if (yType == Integer.class) {
				if (x == inf)
					return inf;
				if (x == minus_inf)
					return minus_inf;
				return ((BigDecimal) x).subtract(BigDecimal.valueOf((Integer) y), mc);
			}
			if (yType == BigDecimal.class) {
				if (x == inf)
					return y == inf ? 0 : inf;
				if (x == minus_inf)
					return y == minus_inf ? 0 : minus_inf;
				if (y == inf)
					return minus_inf;
				if (y == minus_inf)
					return inf;
				return ((BigDecimal) x).subtract((BigDecimal) y, mc);
			}
		}
		return sub(toNum(x), toNum(y));
	}

	private static final Integer one = 1;
	public static Number add1(Object x) {
		return add(x, one);
	}
	public static Number sub1(Object x) {
		return sub(x, one);
	}

	public static Number mul(Object x, Object y) {
		if (x instanceof Integer && y instanceof Integer)
			return (Integer) x * (Integer) y;
		return mul2(x, y);
	}

	private static Number mul2(Object x, Object y) {
		Class<?> xType = x.getClass();
		Class<?> yType = y.getClass();
		if (xType == Integer.class) {
			if (yType == BigDecimal.class) {
				if ((Integer) x == 0)
					return 0;
				if (y == inf)
					return (Integer) x > 0 ? inf : minus_inf;
				if (y == minus_inf)
					return (Integer) x > 0 ? minus_inf : inf;
				return BigDecimal.valueOf((Integer) x).multiply((BigDecimal) y, mc);
			}
		} else if (xType == BigDecimal.class) {
			if (yType == Integer.class) {
				if ((Integer) y == 0)
					return 0;
				if (x == inf)
					return (Integer) y > 0 ? inf : minus_inf;
				if (x == minus_inf)
					return (Integer) y > 0 ? minus_inf : inf;
				return ((BigDecimal) x).multiply(BigDecimal.valueOf((Integer) y), mc);
			}
			if (yType == BigDecimal.class) {
				if (((BigDecimal) x).signum() == 0
						|| ((BigDecimal) y).signum() == 0)
					return 0;
				if (x == inf)
					return ((BigDecimal) y).signum() > 0 ? inf : minus_inf;
				if (x == minus_inf)
					return ((BigDecimal) y).signum() > 0 ? minus_inf : inf;
				if (y == inf)
					return ((BigDecimal) x).signum() > 0 ? inf : minus_inf;
				if (y == minus_inf)
					return ((BigDecimal) x).signum() > 0 ? minus_inf : inf;
				return ((BigDecimal) x).multiply((BigDecimal) y, mc);
			}
		}
		return mul(toNum(x), toNum(y));
	}

	public final static BigDecimal zero = BigDecimal.ZERO;
	public final static BigDecimal inf =
			BigDecimal.valueOf(1, Integer.MAX_VALUE);
	public final static BigDecimal minus_inf =
			BigDecimal.valueOf(-1, Integer.MAX_VALUE);

	public static Number div(Object x, Object y) {
		Class<?> xType = x.getClass();
		Class<?> yType = y.getClass();
		if (xType == Integer.class) {
			if ((Integer) x == 0)
				return 0;
			if (yType == Integer.class) {
				if ((Integer) y == 0)
					return ((Integer) x) < 0 ? minus_inf : inf;
				return BigDecimal.valueOf((Integer) x).divide(
						BigDecimal.valueOf((Integer) y), mc);
			}
			if (yType == BigDecimal.class) {
				if (y == inf || y == minus_inf)
					return 0;
				if (((BigDecimal) y).signum() == 0)
					return ((Integer) x) < 0 ? minus_inf : inf;
				return BigDecimal.valueOf((Integer) x).divide((BigDecimal) y, mc);
			}
		} else if (xType == BigDecimal.class) {
			if (((BigDecimal) x).signum() == 0)
				return 0;
			if (yType == Integer.class) {
				if ((Integer) y == 0)
					return ((BigDecimal) x).signum() < 0 ? minus_inf : inf;
				if (x == inf)
					return (Integer) y >= 0 ? inf : minus_inf;
				if (x == minus_inf)
					return (Integer) y >= 0 ? minus_inf : inf;
				return ((BigDecimal) x).divide(BigDecimal.valueOf((Integer) y), mc);
			}
			if (yType == BigDecimal.class) {
				if (x == inf)
					return y == inf ? 1 : y == minus_inf ? -1
							: ((BigDecimal) y).signum() >= 0 ? inf : minus_inf;
				if (x == minus_inf)
					return y == inf ? -1 : y == minus_inf ? 1
							: ((BigDecimal) y).signum() >= 0 ? minus_inf : inf;
				if (y == inf || y == minus_inf)
					return 0;
				if (((BigDecimal) y).signum() == 0)
					return ((BigDecimal) x).signum() < 0 ? minus_inf : inf;
				return ((BigDecimal) x).divide((BigDecimal) y, mc);
			}
		}
		return div(toNum(x), toNum(y));
	}

	public static Number mod(Object x, Object y) {
		if (x instanceof Integer && y instanceof Integer)
			return (Integer) x % (Integer) y;
		return toInt(x) % toInt(y);
	}

	public static Number uminus(Object x) {
		if (x instanceof Integer)
			return -(Integer) x;
		if (x instanceof BigDecimal)
			return ((BigDecimal) x).negate();
		return uminus(toNum(x));
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
				BigDecimal n = new BigDecimal(s, mc);
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
		if (x == Boolean.TRUE || x == Boolean.FALSE)
			return (Boolean) x;
		throw new SuException("can't convert " + typeName(x) + " to boolean");
	}

	public static int toInt(Object x) {
		Class<?> xType = x.getClass();
		if (xType == Integer.class)
			return ((Integer) x);
		if (xType == BigDecimal.class)
			return toIntBD((BigDecimal) x);
		if (xType == String.class || xType == Concat.class)
			return toIntS(x.toString());
		if (xType == Boolean.class)
			return x == Boolean.TRUE ? 1 : 0;
		throw new SuException("can't convert " + typeName(x) + " to integer");
	}

	public final static BigDecimal INT_MIN = new BigDecimal(Integer.MIN_VALUE);
	public final static BigDecimal INT_MAX = new BigDecimal(Integer.MAX_VALUE);

	public static int toIntBD(BigDecimal n) {
		if (n.compareTo(INT_MIN) == -1)
			return Integer.MIN_VALUE;
		if (n.compareTo(INT_MAX) == 1)
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
		if (n.compareTo(inf) == 0)
			return "Inf";
		if (n.compareTo(minus_inf) == 0)
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

	public static void thrower(Object e) {
		throw new SuException(e);
	}

	public static Object call(Object x, Object... args) {
		try {
			//System.out.println("call " + display(x) + display(args));
			if (x instanceof SuValue)
				return ((SuValue) x).call(args);
			if (x instanceof String) {
				return callString(x, args);
			}
		} catch (java.lang.StackOverflowError e) {
			throw new SuException("function call overflow", e);
		}
		throw new SuException("can't call " + typeName(x));
	}

	static Object callString(Object x, Object... args) {
		// string(object, ...) => object[string](...)
		Object ob = args[0];
		args = Arrays.copyOfRange(args, 1, args.length);
		return invoke(ob, x.toString(), args);
	}

	public static Object invoke(Object x, String method, Object... args) {
		try {
			//System.out.println("invoke " + display(x) + "." + method + display(args));
			if (x instanceof SuValue)
				return ((SuValue) x).invoke(x, method, args);
			Class<?> xType = x.getClass();
			if (xType == String.class)
				return StringMethods.invoke((String) x, method, args);
			if (xType == Integer.class)
				return NumberMethods.invoke((Integer) x, method, args);
			if (xType == Long.class)
				x = BigDecimal.valueOf((Long) x);
			if (xType == BigDecimal.class)
				return NumberMethods.invoke((BigDecimal) x, method, args);
			if (xType == Date.class)
				return DateMethods.invoke((Date) x, method, args);
		} catch (java.lang.StackOverflowError e) {
			throw new SuException("function call overflow");
		}
		throw methodNotFound(x, method);
	}

	public static String toMethodString(Object method) {
		if (isString(method))
			return method.toString().intern();
		throw new SuException("invalid method: " + method);
	}

	// callN and invokeN are to simplify code generation
	// so caller doesn't have to build args array

	public static Object callN(Object it) {
		return call(it);
	}
	public static Object callN(Object it, Object a1) {
		return call(it, a1);
	}
	public static Object callN(Object it, Object a1, Object a2) {
		return call(it, a1, a2);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3) {
		return call(it, a1, a2, a3);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4) {
		return call(it, a1, a2, a3, a4);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5) {
		return call(it, a1, a2, a3, a4, a5);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6) {
		return call(it, a1, a2, a3, a4, a5, a6);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7) {
		return call(it, a1, a2, a3, a4, a5, a6, a7);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84, Object a85) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84, a85);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84, Object a85, Object a86) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84, a85, a86);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84, Object a85, Object a86, Object a87) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84, a85, a86, a87);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84, Object a85, Object a86, Object a87, Object a88) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84, a85, a86, a87, a88);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84, Object a85, Object a86, Object a87, Object a88, Object a89) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84, a85, a86, a87, a88, a89);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84, Object a85, Object a86, Object a87, Object a88, Object a89, Object a90) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84, a85, a86, a87, a88, a89, a90);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84, Object a85, Object a86, Object a87, Object a88, Object a89, Object a90, Object a91) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84, a85, a86, a87, a88, a89, a90, a91);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84, Object a85, Object a86, Object a87, Object a88, Object a89, Object a90, Object a91, Object a92) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84, a85, a86, a87, a88, a89, a90, a91, a92);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84, Object a85, Object a86, Object a87, Object a88, Object a89, Object a90, Object a91, Object a92, Object a93) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84, a85, a86, a87, a88, a89, a90, a91, a92, a93);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84, Object a85, Object a86, Object a87, Object a88, Object a89, Object a90, Object a91, Object a92, Object a93, Object a94) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84, a85, a86, a87, a88, a89, a90, a91, a92, a93, a94);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84, Object a85, Object a86, Object a87, Object a88, Object a89, Object a90, Object a91, Object a92, Object a93, Object a94, Object a95) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84, a85, a86, a87, a88, a89, a90, a91, a92, a93, a94, a95);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84, Object a85, Object a86, Object a87, Object a88, Object a89, Object a90, Object a91, Object a92, Object a93, Object a94, Object a95, Object a96) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84, a85, a86, a87, a88, a89, a90, a91, a92, a93, a94, a95, a96);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84, Object a85, Object a86, Object a87, Object a88, Object a89, Object a90, Object a91, Object a92, Object a93, Object a94, Object a95, Object a96, Object a97) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84, a85, a86, a87, a88, a89, a90, a91, a92, a93, a94, a95, a96, a97);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84, Object a85, Object a86, Object a87, Object a88, Object a89, Object a90, Object a91, Object a92, Object a93, Object a94, Object a95, Object a96, Object a97, Object a98) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84, a85, a86, a87, a88, a89, a90, a91, a92, a93, a94, a95, a96, a97, a98);
	}
	public static Object callN(Object it, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84, Object a85, Object a86, Object a87, Object a88, Object a89, Object a90, Object a91, Object a92, Object a93, Object a94, Object a95, Object a96, Object a97, Object a98, Object a99) {
		return call(it, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84, a85, a86, a87, a88, a89, a90, a91, a92, a93, a94, a95, a96, a97, a98, a99);
	}

	public static Object invokeN(Object it, String method) {
		return invoke(it, method);
	}
	public static Object invokeN(Object it, String method, Object a1) {
		return invoke(it, method, a1);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2) {
		return invoke(it, method, a1, a2);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3) {
		return invoke(it, method, a1, a2, a3);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4) {
		return invoke(it, method, a1, a2, a3, a4);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5) {
		return invoke(it, method, a1, a2, a3, a4, a5);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84, Object a85) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84, a85);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84, Object a85, Object a86) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84, a85, a86);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84, Object a85, Object a86, Object a87) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84, a85, a86, a87);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84, Object a85, Object a86, Object a87, Object a88) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84, a85, a86, a87, a88);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84, Object a85, Object a86, Object a87, Object a88, Object a89) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84, a85, a86, a87, a88, a89);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84, Object a85, Object a86, Object a87, Object a88, Object a89, Object a90) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84, a85, a86, a87, a88, a89, a90);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84, Object a85, Object a86, Object a87, Object a88, Object a89, Object a90, Object a91) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84, a85, a86, a87, a88, a89, a90, a91);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84, Object a85, Object a86, Object a87, Object a88, Object a89, Object a90, Object a91, Object a92) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84, a85, a86, a87, a88, a89, a90, a91, a92);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84, Object a85, Object a86, Object a87, Object a88, Object a89, Object a90, Object a91, Object a92, Object a93) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84, a85, a86, a87, a88, a89, a90, a91, a92, a93);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84, Object a85, Object a86, Object a87, Object a88, Object a89, Object a90, Object a91, Object a92, Object a93, Object a94) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84, a85, a86, a87, a88, a89, a90, a91, a92, a93, a94);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84, Object a85, Object a86, Object a87, Object a88, Object a89, Object a90, Object a91, Object a92, Object a93, Object a94, Object a95) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84, a85, a86, a87, a88, a89, a90, a91, a92, a93, a94, a95);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84, Object a85, Object a86, Object a87, Object a88, Object a89, Object a90, Object a91, Object a92, Object a93, Object a94, Object a95, Object a96) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84, a85, a86, a87, a88, a89, a90, a91, a92, a93, a94, a95, a96);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84, Object a85, Object a86, Object a87, Object a88, Object a89, Object a90, Object a91, Object a92, Object a93, Object a94, Object a95, Object a96, Object a97) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84, a85, a86, a87, a88, a89, a90, a91, a92, a93, a94, a95, a96, a97);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84, Object a85, Object a86, Object a87, Object a88, Object a89, Object a90, Object a91, Object a92, Object a93, Object a94, Object a95, Object a96, Object a97, Object a98) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84, a85, a86, a87, a88, a89, a90, a91, a92, a93, a94, a95, a96, a97, a98);
	}
	public static Object invokeN(Object it, String method, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16, Object a17, Object a18, Object a19, Object a20, Object a21, Object a22, Object a23, Object a24, Object a25, Object a26, Object a27, Object a28, Object a29, Object a30, Object a31, Object a32, Object a33, Object a34, Object a35, Object a36, Object a37, Object a38, Object a39, Object a40, Object a41, Object a42, Object a43, Object a44, Object a45, Object a46, Object a47, Object a48, Object a49, Object a50, Object a51, Object a52, Object a53, Object a54, Object a55, Object a56, Object a57, Object a58, Object a59, Object a60, Object a61, Object a62, Object a63, Object a64, Object a65, Object a66, Object a67, Object a68, Object a69, Object a70, Object a71, Object a72, Object a73, Object a74, Object a75, Object a76, Object a77, Object a78, Object a79, Object a80, Object a81, Object a82, Object a83, Object a84, Object a85, Object a86, Object a87, Object a88, Object a89, Object a90, Object a91, Object a92, Object a93, Object a94, Object a95, Object a96, Object a97, Object a98, Object a99) {
		return invoke(it, method, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30, a31, a32, a33, a34, a35, a36, a37, a38, a39, a40, a41, a42, a43, a44, a45, a46, a47, a48, a49, a50, a51, a52, a53, a54, a55, a56, a57, a58, a59, a60, a61, a62, a63, a64, a65, a66, a67, a68, a69, a70, a71, a72, a73, a74, a75, a76, a77, a78, a79, a80, a81, a82, a83, a84, a85, a86, a87, a88, a89, a90, a91, a92, a93, a94, a95, a96, a97, a98, a99);
	}

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

	public static String catchMatch(SuException e, String patterns) {
		String es = e.toString();
		if (patterns == null)
			return es;
		for (String pat : catchSplitter.split(patterns)) {
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
