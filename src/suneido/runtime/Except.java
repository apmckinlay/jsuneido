/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import java.util.ArrayList;

import com.google.common.collect.Lists;

import suneido.SuObject;
import suneido.SuValue;

/**
 * Type of a variable declared in a Suneido {@code catch} clause.
 * Can be treated as a string for backward compatibility.
 */
public final class Except extends String2 {
	private final String message;
	private final Throwable throwable;

	public Except(Throwable throwable) {
		this(throwable.toString(), throwable);
	}

	public Except(String message, Throwable throwable) {
		assert message != null;
		this.message = message;
		this.throwable = throwable;
	}

	Throwable getThrowable() {
		return throwable;
	}

	// SuValue

	@Override
	public String typeName() {
		return "Except";
	}

	@Override
	public SuValue lookup(String method) {
		SuValue f = methods.getMethod(method);
		return f != null ? f : super.lookup(method);
	}

	// CharSequence

	@Override
	public char charAt(int index) {
		return message.charAt(index);
	}

	@Override
	public int length() {
		return message.length();
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return message.subSequence(start, end);
	}

	@Override
	public String toString() {
		return message;
	}

	// BUILT-IN METHODS

	private static final BuiltinMethods methods = new BuiltinMethods(
			Except.class);

	@Params("string")
	public static Object As(Object self, Object a) {
		return new Except(Ops.toStr(a), ((Except) self).throwable);
	}

	public static Object Callstack(Object self) {
		Throwable e = ((Except) self).throwable;
		ArrayList<StackTraceElement> stack = Lists.newArrayList();
		getStack(e, stack);
		SuObject calls = new SuObject();
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

	private static SuObject callob(StackTraceElement x) {
		SuObject call = new SuObject();
		call.put("fn", x.toString());
		call.put("locals", new SuObject());
		return call;
	}
}
