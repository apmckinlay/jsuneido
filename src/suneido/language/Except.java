/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import suneido.SuContainer;
import suneido.SuValue;

/**
 * This is the value this is assigned to a catch variable.
 * Wraps a java.lang.Exception.
 * Can be treated as a string for backwards compatibility.
 * Derives from {@link Concat} since that is already treated as a string.
 */
public class Except extends Concat {
	private final Exception e;
	private static final BuiltinMethods methods = new BuiltinMethods(Except.class);

	public Except(Exception e) {
		super(e.toString());
		this.e = e;
	}

	public Except(Except except, String s) {
		super(s);
		this.e = except.e;
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
			return new Except((Except) self, Ops.toStr(a));
		}
	}

	public static class Callstack extends SuMethod0 {
		@Override
		public Object eval0(Object self) {
			Exception e = ((Except) self).e;
			SuContainer calls = new SuContainer();
			for (StackTraceElement x : e.getStackTrace())
				calls.add(callob(x));
			return calls;
		}
	}

	private static SuContainer callob(StackTraceElement x) {
		SuContainer call = new SuContainer();
		call.put("fn", x.toString());
		call.put("locals", new SuContainer());
		return call;
	}

}
