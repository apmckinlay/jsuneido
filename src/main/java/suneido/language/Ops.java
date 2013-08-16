/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static suneido.language.Numbers.*;
import static suneido.util.Util.capitalize;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.annotation.concurrent.ThreadSafe;

import suneido.*;
import suneido.language.builtin.DateMethods;
import suneido.language.builtin.NumberMethods;
import suneido.language.builtin.StringMethods;
import suneido.language.jsdi.Buffer;
import suneido.util.StringIterator;

import com.google.common.base.CharMatcher;
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

		// This check is because the left-hand side object might be a String,
		// and String.equals() will return false for non-String.
		if (y instanceof String2 || y instanceof Buffer)
			return y.equals(x);

		// Default: use the equals method of the left-hand side Object.
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
		if (xClass == SuSequence.class) {
			((SuSequence) x).instantiate();
			xClass = SuContainer.class;
		}
		if (yClass == SuRecord.class)
			yClass = SuContainer.class;
		if (yClass == SuSequence.class) {
			((SuSequence) y).instantiate();
			yClass = SuContainer.class;
		}
		if (x instanceof String2) {
			x = x.toString();
			xClass = String.class;
		}
		if (y instanceof String2) {
			y = y.toString();
			yClass = String.class;
		}
		if (xClass == yClass) // most common case e.g. 80%
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
		@Override
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

	// fast path, kept small in hopes of getting inlined
	public static Object cat(Object x, Object y) {
		if (x instanceof String && y instanceof String)
			return cat((String) x, (String) y);
		return cat2(x, y);
	}

	private static final int LARGE = 256;

	private static Object cat(String x, String y) {
		return x.length() + y.length() < LARGE
				? x.concat(y)
				: new Concats(x, y);
	}

	private static Object cat2(Object x, Object y) {
		if (x instanceof Concats && y instanceof String)
			return ((Concats) x).append((String) y);
		return cat(toStr(x), toStr(y));
	}

	public static boolean isString(Object x) {
		// TODO: change to single check for instanceof CharSequence?
		return x instanceof CharSequence;
	}

	// fast path, kept small in hopes of getting inlined
	public static Number add(Object x, Object y) {
		if (x instanceof Integer && y instanceof Integer)
			return narrow((long) (Integer) x + (Integer) y);
		return add2(x, y);
	}

	// fast path, kept small in hopes of getting inlined
	public static Number sub(Object x, Object y) {
		if (x instanceof Integer && y instanceof Integer)
			return narrow((long) (Integer) x - (Integer) y);
		return sub2(x, y);
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
			return narrow((long) (Integer) x * (Integer) y);
		return mul2(x, y);
	}

	public static Number div(Object x, Object y) {
		// no fast path ?
		return div2(x, y);
	}

	public static Number mod(Object x, Object y) {
		return toInt(x) % toInt(y);
	}

	public static Number uminus(Object x) {
		x = toNum(x);
		if (x instanceof Integer) {
			int x_ = (int) x;
			// Avoid two's complement overflow
			return Integer.MIN_VALUE != x_
				? -x_
				: -(long)x_
				;
		}
		if (x instanceof BigDecimal)
			return ((BigDecimal) x).negate(); // TODO: Use Numbers.MC?
		if (x instanceof BigInteger)
			return ((BigInteger) x).negate(); // TODO: Use Numbers.MC?
		if (x instanceof Long) {
			long x_ = (long) x;
			// Avoid two's complement overflow
			return Long.MIN_VALUE != x_
				? -x_
				: new BigDecimal(x_).negate(Numbers.MC)
				;
		}
		if (x instanceof Short) {
			short x_ = (short) x;
			// Avoid two's complement overflow
			return Short.MIN_VALUE != x_
				? -x_
				: -(int)x_ // If have to convert, use a canonical number type
				;
		}
		if (x instanceof Byte) {
			byte x_ = (byte) x;
			// Avoid two's complement overflow
			return Byte.MIN_VALUE != x_
				? -x_
				: -(int)x_ // If have to convert, use a canonical number type
				;
		}
		// TODO: Check for overflow for float and double?
		// TODO: My sense is that it is more likely that a number will be
		//       represented as a Float or Double than as a Short or Byte (when/
		//       how is it even possible to get a Short/Byte number??). From the
		//       point of view of efficiency, it makes sense to move the
		//       Float/Double negation branches further up since they are much
		//       more likely to be taken t han the Short/Byte branches.
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
		// Use unsigned right-shifting to exhibit same behaviour as C-Suneido
		return toInt(x) >>> toInt(y);
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
		throw new SuException("can't convert : " + typeName(x) + " to boolean");
	}

	public static Boolean toBoolean(Object x) {
		if (x instanceof Boolean)
			return (Boolean) x;
		throw new SuException("can't convert " + typeName(x) + " to boolean");
	}

	public static boolean toBoolean_(Object x) {
		if (x == Boolean.TRUE)
			return true;
		else if (x == Boolean.FALSE)
			return false;
		throw new SuException("can't convert " + typeName(x) + " to boolean");
	}

	public static int toInt(Object x) {
		// TODO: determine whether it is possible and desirable in jSuneido
		//       to hold a reference to types Long, Short, and Byte (noting
		//       that literal processing via AstCompile.fold() and Numbers.
		//       stringToNumber() can only produce either an 'int' or a
		//       BigDecimal.
		if (x instanceof Integer || x instanceof Short || x instanceof Byte)
			return ((Number) x).intValue();
		if (x instanceof Long)
			return toIntFromLong((Long) x);
		if (x instanceof BigDecimal)
			return toIntFromBD((BigDecimal) x);
		if (x instanceof BigInteger)
			return toIntFromBI((BigInteger) x);
		if (x instanceof CharSequence)
			return toIntFromString(x.toString());
		if (x instanceof Boolean)
			return x == Boolean.TRUE ? 1 : 0;
		throw new SuException("can't convert " + Ops.typeName(x) + " to integer");
	}

	public static int toIntIfNum(Object x) {
		// TODO: determine whether it is possible and desirable in jSuneido
		//       to hold a reference to types Long, Short, and Byte (noting
		//       that literal processing via AstCompile.fold() and Numbers.
		//       stringToNumber() can only produce either an 'int' or a
		//       BigDecimal.
		if (x instanceof Integer || x instanceof Short || x instanceof Byte)
			return ((Number) x).intValue();
		if (x instanceof Long)
			return toIntFromLong((Long) x);
		if (x instanceof BigDecimal)
			return toIntFromBD((BigDecimal) x);
		if (x instanceof BigInteger)
			return toIntFromBI((BigInteger) x);
		throw new SuException(Ops.typeName(x) + " is not a convertible Number");
	}

	// used by string operations to coerce arguments
	// automatic conversion is only done from booleans and numbers
	public static String toStr(Object x) {
		String s = toStr2(x);
		if (s == null)
			throw new SuException("can't convert " + typeName(x) + " to String");
		return s;
	}

	public static String toStr2(Object x) {
		if (x == Boolean.TRUE)
			return "true";
		if (x == Boolean.FALSE)
			return "false";
		if (x instanceof BigDecimal)
			return toStringBD((BigDecimal) x);
		if (isString(x) || x instanceof Number || x instanceof Buffer)
			return x.toString();
		return null;
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

	public static boolean default_single_quotes = false;

	public static String display(Object x) {
		if (x == null)
			return "null";
		if (isString(x))
			return displayString(x);
		String s = toStr2(x);
		if (s != null)
			return s;
		if (x instanceof Date)
			return toStringDate((Date) x);
		return x.toString();
	}

	static final CharMatcher printable = CharMatcher.inRange(' ', '~');

	private static String displayString(Object x) {
		String s = x.toString();
		if (! s.contains("`") && s.contains("\\") && printable.matchesAllOf(s))
			return "`" + s + "`";
		s = s.replace("\\", "\\\\");
		boolean single_quotes = default_single_quotes
			? !s.contains("'")
			: (s.contains("\"") && !s.contains("'"));
		if (single_quotes)
			return "'" + s + "'";
		else
			return "\"" + s.replace("\"", "\\\"") + "\"";
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

	public static String valueName(Object x) {
		if (null == x)
			throwUninitializedVariable();
		else if (x instanceof SuValue)
			return ((SuValue) x).valueName();
		return "";
	}

	public static int hashCodeContrib(Object x, int nest) {
		if (x instanceof SuValue) {
			return ((SuValue)x).hashCodeContrib(nest);
		} else {
			return x.hashCode();
		}
	}

	public static Throwable exception(Object e) {
		return e instanceof Except
				? new SuException(((Except) e).getThrowable(), e.toString())
				: new SuException(toStr(e));
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
			return getString((CharSequence)x, member);
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
			Method m = x.getClass().getMethod("get" + capitalize(member));
			return m.invoke(x);
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		}
		throw new SuException("get property failed: " + x + "." + member);
	}

	private static Object getArray(Object[] x, int i) {
		return x[i];
	}

	private static Object getString(CharSequence s, Object m) {
		if (m instanceof Range)
			return ((Range) m).substr(s);
		int i = toInt(m);
		int len = s.length();
		if (i < 0)
			i += len;
		return 0 <= i && i < len ? s.subSequence(i, i + 1) : "";
	}

	public static Range rangeTo(Object from, Object to) {
		return new Range.RangeTo(toInt(from), toInt(to));
	}
	public static Range rangeLen(Object from, Object to) {
		return new Range.RangeLen(toInt(from), toInt(to));
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

	public static Except catchMatch(Throwable e) throws Throwable {
		if (e instanceof BlockReturnException)
			throw e;
		return new Except(e);
	}
	public static Except catchMatch(Throwable e, String patterns) throws Throwable {
		if (! (e instanceof BlockReturnException) &&
				catchMatch(e.toString(), patterns))
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
