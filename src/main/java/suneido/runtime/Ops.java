/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import static java.lang.System.identityHashCode;
import static suneido.runtime.Numbers.toDnum;
import static suneido.runtime.Numbers.toNum;

import java.util.*;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ComparisonChain;

import suneido.*;
import suneido.runtime.builtin.NumberMethods;
import suneido.runtime.builtin.StringMethods;
import suneido.util.Dnum;
import suneido.util.RegexCache;
import suneido.util.StringIterator;
import suneido.util.ThreadSafe;

@ThreadSafe // all static methods
public final class Ops {
	public static boolean is_(Object x, Object y) {
		if (x == y)
			return true;

		/* NOTE: cannot compare hashCode's for inequality
		 * because Suneido can compare different types as equal
		 * for example String and Concat or Integer and Dnum */

		// x might be String or Integer,
		// and .equals() won't handle String2 or Dnum
		if (y instanceof String2 || y instanceof Dnum)
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

		if (xClass == yClass && x instanceof Comparable)
			return ((Comparable<Object>) x).compareTo(y); // usual case ~90%

		return cmp2(x, xClass, y, yClass);
	}

	@SuppressWarnings("unchecked")
	private static int cmp2(Object x, Class<?> xClass, Object y, Class<?> yClass) {
		if (xClass == Integer.class) {
			x = Dnum.from((int) x);
			xClass = Dnum.class;
		} else if (x instanceof CharSequence) {
			x = x.toString();
			xClass = String.class;
		} else if (x instanceof SequenceBase) {
			((SequenceBase) x).ck_instantiate();
			xClass = SuContainer.class;
		} else if (xClass == SuRecord.class)
			xClass = SuContainer.class;

		if (yClass == Integer.class) {
			y = Dnum.from((int) y);
			yClass = Dnum.class;
		} else if (y instanceof CharSequence) {
			y = y.toString();
			yClass = String.class;
		} else if (y instanceof SequenceBase) {
			((Sequence) y).ck_instantiate();
			yClass = SuContainer.class;
		} else if (yClass == SuRecord.class)
			yClass = SuContainer.class;

		if (xClass == yClass && x instanceof Comparable)
			return ((Comparable<Object>) x).compareTo(y);

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

		if (xClass == SuDate.class)
			return -1;
		if (yClass == SuDate.class)
			return +1;

		if (xClass == SuContainer.class)
			return -1;
		if (yClass == SuContainer.class)
			return +1;

		return ComparisonChain.start()
				.compare(xClass.toString(), yClass.toString())
				.compare(identityHashCode(x), identityHashCode(y))
				.result();
	}

	public static final class Comp implements Comparator<Object> {
		@Override
		public int compare(Object x, Object y) {
			return cmp(x, y);
		}
	}
	public static final Comp comp = new Comp();

	public static boolean match_(Object s, Object rx) {
		return null != RegexCache.getPattern(toStr(rx)).firstMatch(toStr(s), 0);
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
			return cat2((String) x, (String) y);
		return cat3(x, y);
	}

	private static final int LARGE = 256;

	private static Object cat2(String x, String y) {
		if (x.length() == 0)
			return y;
		if (y.length() == 0)
			return x;
		return x.length() + y.length() < LARGE
				? x.concat(y)
				: new Concats(x, y);
	}

	private static Object cat3(Object x, Object y) {
		if (x instanceof Concats)
			return ((Concats) x).append(y);
		Object result = cat2(coerceStr(x), coerceStr(y));
		if (x instanceof Except)
			return Except.As(x, result);
		if (y instanceof Except)
			return Except.As(y, result);
		return result;
	}

	public static boolean isString(Object x) {
		return x instanceof CharSequence;
	}

	public static Number add(Object x, Object y) {
		if (x instanceof Integer && y instanceof Integer)
			try {
				return Math.addExact((Integer) x, (Integer) y);
			} catch (ArithmeticException unused) {
				// fall through
			}
		return Dnum.add(toDnum(x), toDnum(y));
	}

	public static Number sub(Object x, Object y) {
		if (x instanceof Integer && y instanceof Integer)
			try {
				return Math.subtractExact((Integer) x, (Integer) y);
			} catch (ArithmeticException unused) {
				// fall through
			}
		return Dnum.sub(toDnum(x), toDnum(y));
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
			try {
				return Math.multiplyExact((Integer) x, (Integer) y);
			} catch (ArithmeticException unused) {
				// fall through
			}
		return Dnum.mul(toDnum(x), toDnum(y));
	}

	public static Number div(Object x, Object y) {
		if (x instanceof Integer && y instanceof Integer &&
				(int) y != 0 && (int) x % (int) y == 0)
			return (int) x / (int) y;
		return Dnum.div(toDnum(x), toDnum(y));
	}

	public static Number mod(Object x, Object y) {
		return toInt(x) % toInt(y);
	}

	public static Number uminus(Object x) {
		x = toNum(x);
		if (x instanceof Integer)
			return -(int) x; // ignore overflow
		if (x instanceof Dnum)
			return ((Dnum) x).negate();
		throw SuInternalError.unreachable();
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

	public static int toIntBool(Object x) {
		if (x == Boolean.TRUE)
			return 1;
		if (x == Boolean.FALSE)
			return 0;
		throw new SuException("expected boolean, got " + typeName(x));
	}

	public static Boolean toBoolean(Object x) {
		if (x instanceof Boolean)
			return (Boolean) x;
		throw new SuException("expected boolean, got " + typeName(x));
	}

	public static boolean toBoolean_(Object x) {
		if (x == Boolean.TRUE)
			return true;
		else if (x == Boolean.FALSE)
			return false;
		throw new SuException("expected boolean, got " + typeName(x));
	}

	/** Used for bit and range operations */
	public static int toInt(Object x) {
		if (x instanceof Integer)
			return (int) x;
		if (x instanceof Dnum)
			return ((Dnum) x).intValue();
		return likeZero(x);
	}

	static int likeZero(Object x) {
		if (x == Boolean.FALSE ||
				(x instanceof CharSequence && ((CharSequence) x).length() == 0))
			return 0;
		throw new SuException((x == Boolean.TRUE)
			? "can't convert true to number"
			: "can't convert " + Ops.typeName(x) + " to number");
	}

	public static String toStr(Object x) {
		if (x instanceof CharSequence)
			return x.toString();
		throw new SuException("can't convert " + typeName(x) + " to String");
	}

	public static String coerceStr(Object x) {
		if (x == Boolean.TRUE)
			return "true";
		if (x == Boolean.FALSE)
			return "false";
		if (isString(x) || x instanceof Number)
			return x.toString();
		if (x instanceof SuInstance) {
			String s = ((SuInstance) x).userDefToString();
			if (s != null)
				return s;
		}
		throw new SuException("can't convert " + typeName(x) + " to String");
	}

	public static boolean default_single_quotes = false;

	public static String display(Object x) {
		if (x == null)
			return "null";
		// need to check for string before SuValue
		// because Concats and Buffer are SuValue but don't have display
		// could add display but need to handle String here anyway
		// since it's not SuValue
		if (isString(x))
			return displayString(x);
		if (x instanceof SuValue)
			return ((SuValue) x).display();
		return x.toString();
	}

	static final CharMatcher printable = CharMatcher.inRange(' ', '~');

	private static String displayString(Object x) {
		String s = x.toString();
		if (! s.contains("`") && s.contains("\\") && printable.matchesAllOf(s))
			return "`" + s + "`";
		s = s.replace("\\", "\\\\").replace("\000", "\\x00");
		boolean single_quotes = default_single_quotes
			? !s.contains("'")
			: (s.contains("\"") && !s.contains("'"));
		if (single_quotes)
			return "'" + s + "'";
		else
			return "\"" + s.replace("\"", "\\\"") + "\"";
	}

	public static SuContainer toContainer(Object x) {
		return x instanceof SuValue ? ((SuValue) x).toContainer() : null;
	}

	public static String typeName(Object x) {
		if (x == null)
			return "uninitialized";
		if (x instanceof SuValue)
			return ((SuValue) x).typeName();
		Class<?> xClass = x.getClass();
		if (xClass == String.class)
			return "String";
		if (xClass == Boolean.class)
			return "Boolean";
		if (xClass == Integer.class || xClass == Dnum.class)
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

	public static int hashCodeContrib(Object x) {
		if (x instanceof SuValue) {
			return ((SuValue)x).hashCodeContrib();
		} else {
			return x.hashCode();
		}
	}

	public static Throwable exception(Object e) {
		return e instanceof Except
				? new SuException(e.toString(), ((Except) e).getThrowable(), true)
				: new SuException(toStr(e));
	}

	/** calls are generated via AstCompile.needNullCheck */
	public static void throwUninitializedVariable() {
		throw new SuException("uninitialized variable");
	}
	/** calls are generated via AstCompile.needNullCheck */
	public static void throwNoReturnValue() {
		throw new SuException("no return value");
	}
	/** calls are generated via AstCompile.needNullCheck */
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
		ArgsIterator iter = new ArgsIterator(args);
		if (!iter.hasNext())
			throw callStringNoThis();
		Object ob = iter.next();
		if (ob instanceof Map.Entry)
			throw callStringNoThis();
		return invoke(ob, x.toString(), iter.rest());
	}

	private static SuException callStringNoThis() {
		return new SuException("string call requires 'this' argument");
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
		if (x instanceof Number) // e.g. Integer, Dnum
			return NumberMethods.singleton;
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
			throw new SuException(typeName(x) + " does not support put (" + member + ")");
	}

	public static Object get(Object x, Object member) {
		if (x == null || member == null)
			throw new SuException("uninitialized");
		if (x instanceof SuValue) {
			Object y = ((SuValue) x).get(member);
			if (y == null)
				throw new SuException("uninitialized member: " + member);
			return y;
		} else if (isString(x))
			return getString((CharSequence) x, member);
		throw new SuException(typeName(x) + " does not support get (" + member + ")");
	}

	private static Object getString(CharSequence s, Object m) {
		int i = Integer.MIN_VALUE;
		if (m instanceof Integer)
			i = (int) m;
		else if (m instanceof Dnum)
			i = ((Dnum) m).intOrMin();
		if (i == Integer.MIN_VALUE)
			throw new SuException("string subscripts must be integers");
		int len = s.length();
		if (i < -len || len < i)
			return "";
		if (i < 0)
			i += len;
		return 0 <= i && i < len ? s.subSequence(i, i + 1) : "";
	}

	public static Object rangeTo(Object x, Object i, Object j) {
		if (x instanceof CharSequence)
			return rangeToString((CharSequence) x, toInt(i), toInt(j));
		else if (x instanceof SuValue)
			return ((SuValue) x).rangeTo(toInt(i), toInt(j));
		else
			throw new SuException(typeName(x) + " "
					+ " does not support range");
	}
	private static Object rangeToString(CharSequence s, int i, int j) {
		int size = s.length();
		int f = Range.prepFrom(i, size);
		int t = Range.prepTo(f, j, size);
		return s.subSequence(f, t);
	}

	public static Object rangeLen(Object x, Object i, Object n) {
		if (x instanceof CharSequence)
			return rangeLenString((CharSequence) x, toInt(i), toInt(n));
		else if (x instanceof SuValue)
			return ((SuValue) x).rangeLen(toInt(i), toInt(n));
		else
			throw new SuException(typeName(x) + " "
					+ " does not support range");
	}

	private static Object rangeLenString(CharSequence s, int i, int n) {
		int size = s.length();
		int f = Range.prepFrom(i, size);
		int t = f + Range.prepLen(n, size - f);
		return s.subSequence(f, t);
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
		return catchMatch(e, "");
	}
	public static Except catchMatch(Throwable e, String patterns) throws Throwable {
		if (Suneido.exiting && Thread.currentThread().isDaemon())
			System.exit(0); // should just block since we are already exiting
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
