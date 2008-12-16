package suneido;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.*;


/**
 * Miscellaneous functions.
 *
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public class Util {

	@SuppressWarnings("unchecked")
	public static <T> String listToCommas(List<T> list) {
		if (list == null || list.isEmpty())
			return "";
		StringBuilder sb = new StringBuilder();
		for (T x : list) {
			if (x instanceof List)
				sb.append(listToParens((List<String>) x));
			else
				sb.append(x);
			sb.append(",");
		}
		return sb.deleteCharAt(sb.length() - 1).toString();
	}

	public static <T> String listToParens(List<T> list) {
		return "(" + listToCommas(list) + ")";
	}

	public static List<String> commasToList(String s) {
		if (s.equals(""))
			return Collections.emptyList();
		return Arrays.asList(s.split(","));
	}

	public static String bufferToString(ByteBuffer buf) {
		byte[] bytes = new byte[buf.remaining()];
		int pos = buf.position();
		buf.get(bytes);
		buf.position(pos);
		try {
			return new String(bytes, "US-ASCII");
		} catch (UnsupportedEncodingException e) {
			throw new SuException("can't convert buffer to string", e);
		}
	}

	public static ByteBuffer stringToBuffer(String s) {
		return ByteBuffer.wrap(s.getBytes());
	}

	/**
	 * @return A new list containing all the values from x and y. x is copied as
	 *         is, so if it has duplicates they are retained. Duplicates from y
	 *         are not retained.
	 */
	public static <T> List<T> union(List<T> x, List<T> y) {
		return addUnique(new ArrayList<T>(x), y);
	}

	public static <T> List<T> addUnique(List<T> x, List<T> y) {
		for (T s : y)
			if (!x.contains(s))
				x.add(s);
		return x;
	}

	public static <T> List<T> addUnique(List<T> list, T x) {
		if (!list.contains(x))
			list.add(x);
		return list;
	}

	public static <T> List<T> removeDups(List<T> x) {
		List<T> result = new ArrayList<T>();
		for (T s : x)
			if (!result.contains(s))
				result.add(s);
		return result;
	}

	public static <T> List<T> difference(List<T> x, List<T> y) {
		List<T> result = new ArrayList<T>();
		for (T s : x)
			if (!y.contains(s))
				result.add(s);
		return result;
	}

	public static <T> List<T> intersect(List<T> x, List<T> y) {
		List<T> result = new ArrayList<T>();
		for (T s : x)
			if (y.contains(s))
				result.add(s);
		return result;
	}

	public static <T> boolean prefix(List<T> x, List<T> y) {
		if (y.size() > x.size())
			return false;
		for (int i = 0; i < y.size(); ++i)
			if (!x.get(i).equals(y.get(i)))
				return false;
		return true;
	}

	public static <T> boolean prefix_set(List<T> list, List<T> set) {
		int set_size = set.size();
		if (list.size() < set_size)
			return false;
		for (int i = 0; i < set_size; ++i)
			if (!set.contains(list.get(i)))
				return false;
		return true;
	}

	public static <T> boolean set_eq(List<T> x, List<T> y) {
		int n = 0;
		for (T s : x)
			if (y.contains(s))
				++n;
		return n == x.size() && n == y.size();
	}

	public static <T> boolean nil(List<T> x) {
		return x == null || x.isEmpty();
	}

	public static <T> List<T> concat(List<T> x, List<T> y) {
		List<T> result = new ArrayList<T>(x);
		result.addAll(y);
		return result;
	}

	public static <T> List<T> remove(List<T> list, T x) {
		List<T> result = new ArrayList<T>();
		for (T y : list)
			if (x == null ? x != y : !x.equals(y))
				result.add(y);
		return result;
	}

	public static <T> List<T> list(T a) {
		return Collections.singletonList(a);
	}
	public static <T> List<T> list(T a, T b) {
		List<T> list = new ArrayList<T>(2);
		list.add(a);
		list.add(b);
		return list;
	}
	public static <T> List<T> list(T a, T b, T c) {
		List<T> list = new ArrayList<T>(3);
		list.add(a);
		list.add(b);
		list.add(c);
		return list;
	}

	public static <T> List<T> list(T a, T b, T c, T d) {
		List<T> list = new ArrayList<T>(4);
		list.add(a);
		list.add(b);
		list.add(c);
		list.add(d);
		return list;
	}

	public static <T> List<T> list(T a, T b, T c, T d, T e) {
		List<T> list = new ArrayList<T>(5);
		list.add(a);
		list.add(b);
		list.add(c);
		list.add(d);
		list.add(e);
		return list;
	}

	public static <T> List<T> list(T a, T b, T c, T d, T e, T f) {
		List<T> list = new ArrayList<T>(6);
		list.add(a);
		list.add(b);
		list.add(c);
		list.add(d);
		list.add(e);
		list.add(f);
		return list;
	}

	public static <T> List<T> list(T a, T b, T c, T d, T e, T f, T g) {
		List<T> list = new ArrayList<T>(7);
		list.add(a);
		list.add(b);
		list.add(c);
		list.add(d);
		list.add(e);
		list.add(f);
		list.add(g);
		return list;
	}

	public static <T> List<T> list(T a, T b, T c, T d, T e, T f, T g, T h) {
		List<T> list = new ArrayList<T>(8);
		list.add(a);
		list.add(b);
		list.add(c);
		list.add(d);
		list.add(e);
		list.add(f);
		list.add(g);
		list.add(h);
		return list;
	}

}
