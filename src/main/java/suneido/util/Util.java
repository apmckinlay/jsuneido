/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.util;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Lists;

import suneido.runtime.Ops;

/**
 * Miscellaneous functions.
 */
@ThreadSafe
public class Util {
	/** Readline limit for SuFile, SocketClient, and RunPiped.
	 * Should be consistent with cSuneido.
	 */
	public static final int MAX_LINE = 4000;

	public static boolean isCapitalized(String s) {
		return Character.isUpperCase(s.charAt(0));
	}

	public static String capitalize(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	public static String uncapitalize(String s) {
		return s.substring(0, 1).toLowerCase() + s.substring(1);
	}

	/***
	 * Return the contents of a StringBuilder with any trailing returns removed.
	 * <p>
	 * Used by Readline in SuFile, RunPiped, and SocketClient
	 */
	public static String toLine(StringBuilder sb) {
		int n = sb.length();
		while (n > 0 && sb.charAt(n - 1) == '\r')
			--n;
		return sb.substring(0, n);
	}

	@SuppressWarnings("unchecked")
	public static <T> String listToCommas(Collection<T> list) {
		if (list == null || list.isEmpty())
			return "";
		CommaStringBuilder sb = new CommaStringBuilder();
		for (T x : list)
			sb.add(x instanceof List
				? listToParens((List<String>) x)
				: Ops.toStr(x));
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	public static <T> String displayListToCommas(Collection<T> list) {
		if (list == null || list.isEmpty())
			return "";
		CommaStringBuilder sb = new CommaStringBuilder();
		for (T x : list)
			sb.add(x instanceof List
				? displayListToParens((List<String>) x)
				: Ops.display(x));
		return sb.toString();
	}

	public static <T> String listToParens(Collection<T> list) {
		return "(" + listToCommas(list) + ")";
	}

	public static <T> String displayListToParens(Collection<T> list) {
		return "(" + displayListToCommas(list) + ")";
	}

	public static final Joiner commaJoiner = Joiner.on(",");

	public static final Splitter commaSplitter = Splitter.on(',').trimResults();

	public static Iterable<String> commaSplitter(String s) {
		if (s.isEmpty())
			return Collections.emptyList();
		else
			return commaSplitter.split(s);
	}

	public static List<String> commasToList(String s) {
		return Lists.newArrayList(commaSplitter(s));
	}

	/** NOTE: inefficient space-wise - uses one char (2 bytes) per byte */
	public static String bytesToString(byte[] bytes) {
		return bytesToString(bytes, bytes.length);
	}

	/** NOTE: inefficient space-wise - uses one char (2 bytes) per byte */
	public static String bytesToString(byte[] bytes, int len) {
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; ++i)
			sb.append((char) (bytes[i] & 0xff));
		return sb.toString();
	}

	/** NOTE: inefficient space-wise - uses one char (2 bytes) per byte */
	public static String bytesToString(ByteBuffer buf) {
		StringBuilder sb = new StringBuilder(buf.remaining());
		for (int i = buf.position(); i < buf.limit(); ++i)
			sb.append((char) (buf.get(i) & 0xff));
		return sb.toString();
	}

	public static byte[] stringToBytes(String s) {
		byte[] bytes = new byte[s.length()];
		for (int i = 0; i < s.length(); ++i)
			bytes[i] = (byte) s.charAt(i);
		return bytes;
	}

	/**
	 * @return A new list containing all the values from x and y. <b>x is copied as
	 *         is</b>, so if it has duplicates they are retained. Duplicates from y
	 *         are not retained.
	 */
	public static <T> List<T> union(List<T> x, List<T> y) {
		return addAllUnique(new ArrayList<>(x), y);
	}

	public static <T> ImmutableSet<T> setUnion(Collection<T> x, Collection<T> y) {
		Builder<T> builder = new ImmutableSet.Builder<T>();
		if (x != null)
			builder.addAll(x);
		if (y != null)
			builder.addAll(y);
		return builder.build();
	}

	public static <T> ImmutableSet<T> setIntersect(Collection<T> x, Collection<T> y) {
		if (y instanceof Set) {
			Collection<T> tmp = x; x = y; y = tmp;
		}
		ImmutableSet.Builder<T> builder = ImmutableSet.builder();
		for (T e : y)
			if (x.contains(e))
				builder.add(e);
		return builder.build();
	}

	/** modifies list */
	public static <T> List<T> addAllUnique(List<T> list, List<T> x) {
		for (T s : x)
			if (!list.contains(s))
				list.add(s);
		return list;
	}

	/** modifies list */
	public static <T> List<T> addUnique(List<T> list, T x) {
		if (!list.contains(x))
			list.add(x);
		return list;
	}

	/** returns a new list */
	public static <T> List<T> withoutDups(List<T> x) {
		List<T> result = new ArrayList<>();
		for (T s : x)
			if (!result.contains(s))
				result.add(s);
		return result;
	}

	/** returns a new list */
	public static <T> List<T> difference(List<T> x, List<T> y) {
		List<T> result = new ArrayList<>();
		for (T s : x)
			if (!y.contains(s))
				result.add(s);
		return result;
	}

	public static <T> ImmutableSet<T> setDifference(Collection<T> x, Collection<T> y) {
		ImmutableSet.Builder<T> builder = ImmutableSet.builder();
		for (T s : x)
			if (!y.contains(s))
				builder.add(s);
		return builder.build();
	}

	/** @return A new list */
	public static <T> List<T> intersect(List<T> x, List<T> y) {
		List<T> result = new ArrayList<>();
		for (T s : x)
			if (y.contains(s))
				result.add(s);
		return result;
	}

	/** @return Whether or not the first list starts with the second */
	public static <T> boolean startsWith(List<T> x, List<T> y) {
		if (y == null)
			return true; // to match cSuneido prefix
		if (x == null)
			return false; // to match cSuneido prefix
		if (y.size() > x.size())
			return false;
		for (int i = 0; i < y.size(); ++i)
			if (! x.get(i).equals(y.get(i)))
				return false;
		return true;
	}

	public static <T> boolean startsWithSet(List<T> list, List<T> set) {
		int set_size = set.size();
		if (list.size() < set_size)
			return false;
		for (int i = 0; i < set_size; ++i)
			if (!set.contains(list.get(i)))
				return false;
		return true;
	}

	public static <T> boolean setEquals(Collection<T> x, Collection<T> y) {
		if (y instanceof Set) {
			Collection<T> tmp = x; x = y; y = tmp;
		}
		int n = 0;
		for (T s : y)
			if (x.contains(s))
				++n;
		return n == x.size() && n == y.size();
	}

	public static <T> boolean nil(Collection<T> x) {
		return x == null || x.isEmpty();
	}

	/** @return A new list containing the two lists concatenated */
	public static <T> List<T> concat(List<T> x, List<T> y) {
		List<T> result = new ArrayList<>(x);
		result.addAll(y);
		return result;
	}

	/** @return A new list with all occurrences of a value removed */
	public static <T> List<T> without(List<T> list, T x) {
		List<T> result = new ArrayList<>();
		for (T y : list)
			if (x == null ? y != null : ! x.equals(y))
				result.add(y);
		return result;
	}

	@SafeVarargs
	public static <T> T[] array(T... values) {
		return values;
	}

	/**
	 * Based on C++ STL code.
	 *
	 * @return The <u>first</u> position where value could be inserted without
	 *         changing the ordering.
	 */
	public static <T extends Comparable<? super T>> int lowerBound(
			List<T> list, T value) {
		int first = 0;
		int len = list.size();
		while (len > 0) {
			int half = len >> 1;
			int middle = first + half;
			if (list.get(middle).compareTo(value) < 0) {
				first = middle + 1;
				len -= half + 1;
			} else
				len = half;
		}
		return first;
	}
	public static <T> int lowerBound(
			List<T> list, T value, Comparator<? super T> comp) {
		int first = 0;
		int len = list.size();
		while (len > 0) {
			int half = len >> 1;
			int middle = first + half;
			if (comp.compare(list.get(middle), value) < 0) {
				first = middle + 1;
				len -= half + 1;
			} else
				len = half;
		}
		return first;
	}
	public static <T extends Comparable<? super T>> int lowerBound(
			T[] list, T value) {
		int first = 0;
		int len = list.length;
		while (len > 0) {
			int half = len >> 1;
			int middle = first + half;
			if (list[middle].compareTo(value) < 0) {
				first = middle + 1;
				len -= half + 1;
			} else
				len = half;
		}
		return first;
	}
	public static <T> int lowerBound(
			T[] list, T value, Comparator<? super T> cmp) {
		int first = 0;
		int len = list.length;
		while (len > 0) {
			int half = len >> 1;
			int middle = first + half;
			if (cmp.compare(list[middle], value) < 0) {
				first = middle + 1;
				len -= half + 1;
			} else
				len = half;
		}
		return first;
	}
	public static int lowerBound(int[] list, int value, IntComparator cmp) {
		int first = 0;
		int len = list.length;
		while (len > 0) {
			int half = len >> 1;
			int middle = first + half;
			if (cmp.compare(list[middle], value) < 0) {
				first = middle + 1;
				len -= half + 1;
			} else
				len = half;
		}
		return first;
	}

	/**
	 * Based on C++ STL code.
	 *
	 * @return The <u>last</u> position where slot could be inserted without
	 *         changing the ordering.
	 */
	public static <T extends Comparable<? super T>> int upperBound(
			List<T> list, T value) {
		int first = 0;
		int len = list.size();
		while (len > 0) {
			int half = len >> 1;
			int middle = first + half;
			if (value.compareTo(list.get(middle)) < 0)
				len = half;
			else {
				first = middle + 1;
				len -= half + 1;
			}
		}
		return first;
	}
	public static <T> int upperBound(
			List<T> list, T value, Comparator<? super T> comp) {
		int first = 0;
		int len = list.size();
		while (len > 0) {
			int half = len >> 1;
			int middle = first + half;
			if (comp.compare(value, list.get(middle)) < 0)
				len = half;
			else {
				first = middle + 1;
				len -= half + 1;
			}
		}
		return first;
	}
	public static <T> int upperBound(
			T[] list, T value, Comparator<? super T> comp) {
		int first = 0;
		int len = list.length;
		while (len > 0) {
			int half = len >> 1;
			int middle = first + half;
			if (comp.compare(value, list[middle]) < 0)
				len = half;
			else {
				first = middle + 1;
				len -= half + 1;
			}
		}
		return first;
	}
	public static int upperBound(int[] list, int value, IntComparator cmp) {
		int first = 0;
		int len = list.length;
		while (len > 0) {
			int half = len >> 1;
			int middle = first + half;
			if (cmp.compare(value, list[middle]) < 0)
				len = half;
			else {
				first = middle + 1;
				len -= half + 1;
			}
		}
		return first;
	}

	/**
	 * Based on C++ STL code.
	 *
	 * Equivalent to Range(lowerBound, upperBound)
	 *
	 * @return The largest subrange in which value could be inserted at any
	 *         place in it without changing the ordering.
	 */
	public static <T extends Comparable<? super T>> Range equalRange(
			List<T> list, T value) {
		int first = 0;
		int len = list.size();
		while (len > 0) {
			int half = len >> 1;
			int middle = first + half;
			T midvalue = list.get(middle);
			if (midvalue.compareTo(value) < 0) {
				first = middle + 1;
				len -= half + 1;
			} else if (value.compareTo(midvalue) < 0)
				len = half;
			else {
				int left = first + lowerBound(list.subList(first, middle), value);
				++middle;
				int right = middle + upperBound(list.subList(middle, first + len), value);
				return new Range(left, right);
			}
		}
		return new Range(first, first);
	}
	public static <T> Range equalRange(
			List<T> list, T value, Comparator<? super T> comp) {
		int first = 0;
		int len = list.size();
		while (len > 0) {
			int half = len >> 1;
			int middle = first + half;
			T midvalue = list.get(middle);
			if (comp.compare(midvalue, value) < 0) {
				first = middle;
				++first;
				len -= half + 1;
			} else if (comp.compare(value, midvalue) < 0)
				len = half;
			else {
				int left = first + lowerBound(list.subList(first, middle), value, comp);
				++middle;
				int right = middle + upperBound(list.subList(middle, first + len), value, comp);
				return new Range(left, right);
			}
		}
		return new Range(first, first);
	}

	@ThreadSafe
	public static final class Range {
		public final int left;
		public final int right;

		public Range(int left, int right) {
			this.left = left;
			this.right = right;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other)
				return true;
			if (!(other instanceof Range))
				return false;
			Range r = (Range) other;
			return left == r.left && right == r.right;
		}

		@Override
		public int hashCode() {
			return left ^ right;
		}

		@Override
		public String toString() {
			return "Range(" + left + "," + right + ")";
		}
	}

	@GuardedBy("this")
	private static SimpleDateFormat datefmt = new SimpleDateFormat("yyyyMMdd.HHmmssSSS");

	synchronized public static String displayDate(Date date){
		return date == null ? "null" : datefmt.format(date);
	}
}
