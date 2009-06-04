package suneido.util;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.*;

import suneido.SuException;
import suneido.language.Ops;

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
				sb.append(Ops.toStr(x));
			sb.append(",");
		}
		return sb.deleteCharAt(sb.length() - 1).toString();
	}

	@SuppressWarnings("unchecked")
	public static <T> String displayListToCommas(List<T> list) {
		if (list == null || list.isEmpty())
			return "";
		StringBuilder sb = new StringBuilder();
		for (T x : list) {
			if (x instanceof List)
				sb.append(displayListToParens((List<String>) x));
			else
				sb.append(Ops.display(x));
			sb.append(",");
		}
		return sb.deleteCharAt(sb.length() - 1).toString();
	}

	public static <T> String listToParens(List<T> list) {
		return "(" + listToCommas(list) + ")";
	}

	public static <T> String displayListToParens(List<T> list) {
		return "(" + displayListToCommas(list) + ")";
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
			return new String(bytes, "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			throw new SuException("can't convert buffer to string", e);
		}
	}

	public static ByteBuffer stringToBuffer(String s) {
		return ByteBuffer.wrap(s.getBytes());
	}

	public static String bufferToHex(ByteBuffer buf) {
		String s = "";
		for (int i = buf.position(); i < buf.limit(); ++i)
			s += String.format("%02x", buf.get(i)) + " ";
		return s.substring(0, s.length() - 1);
	}

	public static int bufferUcompare(ByteBuffer b1, ByteBuffer b2) {
		int n = Math.min(b1.remaining(), b2.remaining());
		int b1pos = b1.position();
		int b2pos = b2.position();
		for (int i = 0; i < n; ++i) {
			int cmp = (b1.get(b1pos + i) & 0xff) - (b2.get(b2pos + i) & 0xff);
			if (cmp != 0)
				return cmp;
		}
		return b1.remaining() - b2.remaining();
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
			if (x == null ? y != null : !x.equals(y))
				result.add(y);
		return result;
	}

	public static <T> List<T> list(T... values) {
		return Arrays.asList(values);
	}

	public static <T> T[] array(T... values) {
		return values;
	}

}
