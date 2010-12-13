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
public final class Concat extends SuValue implements Comparable<Concat> {
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

	@Override
	public Object call(Object... args) {
		return Ops.callString(toString(), args);
	}

	@Override
	public Object get(Object member) {
		return Ops.get(toString(), member);
	}

	private static int len(Object x) {
		return x instanceof String ? ((String) x).length()
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
		return other.equals(toString());
	}

	public int compareTo(Concat other) {
		return toString().compareTo(other.toString());
	}

	@Override
	public int packSize(int nest) {
		return Pack.packSizeString(toString());
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
		StringBuffer sb = new StringBuffer(len);
		flattenTo(sb);
		left = sb.toString();
		right = null;
		return sb.toString();
	}

	private void flattenTo(StringBuffer sb) {
		if (left instanceof String)
			sb.append((String) left);
		else
			((Concat) left).flattenTo(sb);
		if (right instanceof String)
			sb.append((String) right);
		else if (right != null)
			((Concat) right).flattenTo(sb);
	}

	@Override
	public Object invoke(Object self, String method, Object... args) {
		assert this == self;
		return StringMethods.singleton.invoke(toString(), method, args);
	}

	@Override
	public Object invoke0(Object self, String method) {
		return StringMethods.singleton.invoke0(toString(), method);
	}
	@Override
	public Object invoke1(Object self, String method, Object a) {
		return StringMethods.singleton.invoke1(toString(), method, a);
	}
	@Override
	public Object invoke2(Object self, String method, Object a, Object b) {
		return StringMethods.singleton.invoke2(toString(), method, a, b);
	}
	@Override
	public Object invoke3(Object self, String method, Object a, Object b,
			Object c) {
		return StringMethods.singleton.invoke3(toString(), method, a, b, c);
	}
	@Override
	public Object invoke4(Object self, String method, Object a, Object b,
			Object c, Object d) {
		return StringMethods.singleton.invoke4(toString(), method, a, b, c, d);
	}
	@Override
	public Object invoke5(Object self, String method, Object a, Object b,
			Object c, Object d, Object e) {
		return StringMethods.singleton.invoke5(toString(), method, a, b, c, d, e);
	}
	@Override
	public Object invoke6(Object self, String method, Object a, Object b,
			Object c, Object d, Object e, Object f) {
		return StringMethods.singleton.invoke6(toString(), method, a, b, c, d, e, f);
	}
	@Override
	public Object invoke7(Object self, String method, Object a, Object b,
			Object c, Object d, Object e, Object f, Object g) {
		return StringMethods.singleton.invoke7(toString(), method, a, b, c, d, e, f, g);
	}
	@Override
	public Object invoke8(Object self, String method, Object a, Object b,
			Object c, Object d, Object e, Object f, Object g, Object h) {
		return StringMethods.singleton.invoke8(toString(), method, a, b, c, d, e, f, g, h);
	}
	@Override
	public Object invoke9(Object self, String method, Object a, Object b,
			Object c, Object d, Object e, Object f, Object g, Object h, Object i) {
		return StringMethods.singleton.invoke9(toString(), method, a, b, c, d, e, f, g, h, i);
	}

}
