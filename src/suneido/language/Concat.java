/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.ThreadSafe;

import suneido.SuValue;
import suneido.language.builtin.StringMethods;

/**
 * Used by {@link Ops}.cat to optimized concatenation.
 * Logically immutable but not physically because of flattening.
 */
@ThreadSafe
public class Concat extends SuValue implements Comparable<Concat> {
	private Object left; // String or Concat
	private Object right; // String or Concat or null (if flattened)
	private final int len;

	Concat(Object left, Object right) {
		this.left = left;
		this.right = right;
		this.len = len(left) + len(right);
	}

	Concat(String left, String right, int len) {
		this.left = left;
		this.right = right;
		this.len = len;
	}

	Concat(String s) {
		this(s, null, s.length());
	}

	@Override
	public Object call(Object... args) {
		return Ops.callString(toString(), args);
	}

	@Override
	public Object get(Object member) {
		return Ops.get(toString(), member);
	}

	private static int len(Object x) {
		return x instanceof String
				? ((String) x).length()
				: ((Concat) x).length();
	}

	public int length() {
		return len;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		return other != null && other.equals(toString());
	}

	@Override
	public int compareTo(Concat other) {
		return toString().compareTo(other.toString());
	}

	@Override
	public int packSize(int nest) {
		return Pack.packSize(toString());
	}

	@Override
	public void pack(ByteBuffer buf) {
		Pack.packString(toString(), buf);
	}

	@Override
	public String typeName() {
		return "String";
	}

	@Override
	public synchronized String toString() {
		if (right == null)
			return (String) left;
		StringBuilder sb = new StringBuilder(len);
		flatten(this, sb, 0);
		left = sb.toString();
		right = null;
		return (String) left;
	}

	// recurses on right side, iterates on left
	// on the assumption that you mostly concatenate onto the right
	// COULD iterate on right if it's a concat and left isn't
	private static void flatten(Concat c, StringBuilder sb, int pos) {
		while (true) {
			int leftlen = len(c.left);
			if (c.right instanceof String)
				putAt(sb, pos + leftlen, (String) c.right);
			else if (c.right != null)
				Concat.flatten((Concat) c.right, sb, pos + leftlen); // recurse
			if (c.left instanceof String) {
				putAt(sb, pos, (String) c.left);
				break;
			} else
				c = (Concat) c.left; // iterate
		}
	}

	private static void putAt(StringBuilder sb, int pos, String s) {
		if (pos + s.length() > sb.length())
			sb.setLength(pos + s.length());
		sb.replace(pos, pos + s.length(), s);
	}

	@Override
	public SuValue lookup(String method) {
		return StringMethods.singleton.lookup(method);
	}

}
