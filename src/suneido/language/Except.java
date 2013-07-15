/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import java.util.ArrayList;

import suneido.SuContainer;
import suneido.SuValue;

import com.google.common.collect.Lists;

/**
 * This is the value this is assigned to a catch variable. Wraps a
 * java.lang.Throwable. Can be treated as a string for backwards compatibility.
 */
public final class Except extends String2 {
	private final String s;
	private final Throwable e;
	private static final BuiltinMethods methods = new BuiltinMethods(
			Except.class);

	public Except(Throwable e) {
		this.s = e.toString();
		this.e = e;
	}

	public Except(String s, Throwable e) {
		this.s = s;
		this.e = e;
	}

	Throwable getThrowable() {
		return e;
	}

	@Override
	public String typeName() {
		return "Except";
	}

	@Override
	public SuValue lookup(String method) {
		SuValue f = methods.getMethod(method);
		return f != null ? f : super.lookup(method);
	}

	@Params("string")
	public static Object As(Object self, Object a) {
		return new Except(Ops.toStr(a), ((Except) self).e);
	}

	public static Object Callstack(Object self) {
		Throwable e = ((Except) self).e;
		ArrayList<StackTraceElement> stack = Lists.newArrayList();
		getStack(e, stack);
		SuContainer calls = new SuContainer();
		for (int i = stack.size() - 1; i >= 0; --i)
			if (!stack.get(i).toString().contains(".java:"))
				calls.add(callob(stack.get(i)));
		return calls;
	}

	private static void getStack(Throwable e, ArrayList<StackTraceElement> dest) {
		if (e.getCause() != null)
			getStack(e.getCause(), dest); // bottom up
		StackTraceElement[] stackTrace = e.getStackTrace();
		// skip duplication with cause
		int i = stackTrace.length - 1;
		for (int idest = 0; i >= 0 && idest < dest.size()
				&& stackTrace[i].equals(dest.get(idest)); --i, ++idest) {
		}
		for (; i >= 0; --i)
			dest.add(stackTrace[i]); // reverse order
	}

	private static SuContainer callob(StackTraceElement x) {
		SuContainer call = new SuContainer();
		call.put("fn", x.toString());
		call.put("locals", new SuContainer());
		return call;
	}

	//
	// INTERFACE: CharSequence
	//

	@Override
	public char charAt(int index) {
		return s.charAt(index);
	}

	@Override
	public int length() {
		return s.length();
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return s.subSequence(start, end);
	}

	@Override
	public String toString() {
		return s;
	}

}
