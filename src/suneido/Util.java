package suneido;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * Miscellaneous functions.
 *
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public class Util {

	public static <T> String listToCommas(List<T> list) {
		if (list == null || list.isEmpty())
			return "";
		StringBuilder sb = new StringBuilder();
		for (T x : list)
			sb.append(x).append(",");
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

	public static int find(List<String> list, String value) {
		for (int i = 0; i < list.size(); ++i)
			if (list.get(i).equals(value))
				return i;
		return -1;
	}

	public static String bufferToString(ByteBuffer buf) {
		byte[] bytes = new byte[buf.remaining()];
		buf.get(bytes);
		try {
			return new String(bytes, "US-ASCII");
		} catch (UnsupportedEncodingException e) {
			throw new SuException("can't unpack string", e);
		}
	}

	/**
	 * @return A new list containing all the values from x and y. x is copied as
	 *         is, so if it has duplicates they are retained. Duplicates from y
	 *         are not retained.
	 */
	public static <T> List<T> union(List<T> x, List<T> y) {
		return x.size() > y.size()
		? addUnique(new ArrayList<T>(x), y)
				: addUnique(new ArrayList<T>(y), x);
	}

	public static <T> List<T> addUnique(List<T> x, List<T> y) {
		for (T s : y)
			if (!x.contains(s))
				x.add(s);
		return x;
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

	public static <T> boolean prefix_set(List<T> list, List<T> set) {
		int n = set.size();
		int i = 0;
		for (T s : list)
			if (i >= n || ! set.contains(s))
				break ;
		return i == n;
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

}
