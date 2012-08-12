/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import java.util.ArrayList;

import suneido.SuContainer;
import suneido.SuValue;

import com.google.common.collect.Lists;

/**
 * This is the value this is assigned to a catch variable.
 * Wraps a java.lang.Throwable.
 * Can be treated as a string for backwards compatibility.
 * Derives from {@link Concat} since that is already treated as a string.
 */
public class Except extends Concat {
	private final Throwable e;
	private static final BuiltinMethods methods = new BuiltinMethods(Except.class);

	public Except(Throwable e) {
		super(e.toString());
		this.e = e;
	}

	public Except(String s, Throwable e) {
		super(s);
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

	public static class As extends SuMethod1 {
		{ params = FunctionSpec.string; }
		@Override
		public Object eval1(Object self, Object a) {
			return new Except(Ops.toStr(a), ((Except) self).e);
		}
	}

	public static class Callstack extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			Throwable e = ((Except) self).e;
			ArrayList<StackTraceElement> stack = Lists.newArrayList();
			getStack(e, stack);
			SuContainer calls = new SuContainer();
			for (int i = stack.size() - 1; i >= 0; --i)
				if (! stack.get(i).toString().contains(".java:"))
					calls.add(callob(stack.get(i)));
			return calls;
		}

		private void getStack(Throwable e, ArrayList<StackTraceElement> dest) {
			if (e.getCause() != null)
				getStack(e.getCause(), dest); // bottom up
			StackTraceElement[] stackTrace = e.getStackTrace();
			// skip duplication with cause
			int i = stackTrace.length - 1;
			for (int idest = 0; i >= 0 && idest < dest.size() &&
					stackTrace[i].equals(dest.get(idest)); --i, ++idest) {
			}
			for (; i >= 0; --i)
				dest.add(stackTrace[i]); // reverse order
		}
	}

	private static SuContainer callob(StackTraceElement x) {
		SuContainer call = new SuContainer();
		call.put("fn", x.toString());
		call.put("locals", new SuContainer());
		return call;
	}

}
