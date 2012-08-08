/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import suneido.SuValue;

@SuppressWarnings("static-method")
public abstract class SuCallable extends SuValue {
	protected SuClass myClass;
	protected FunctionSpec params;
	protected boolean isBlock = false;
//	protected Context context = ContextLibraries.context;

	@Override
	public SuValue lookup(String method) {
		if (method == "Params")
			return Params;
		return super.lookup(method);
	}

	private static SuValue Params = new SuMethod0() {
		@Override
		public Object eval0(Object self) {
			return ((SuCallable) self).params.params();
		}
	};

	@Override
	public boolean isCallable() {
		return true;
	}

	/**
	 * Supply missing argument from dynamic implicit or default
	 * This is also done by {@link Args} applyDefaults and dynamicImplicits
	 */
	protected Object fillin(int i) {
		assert params != null : "" + this + " has no params";
		if (params.isDynParam(params.params[i])) {
			Object value = Dynamic.getOrNull("_" + params.params[i]);
			if (value != null)
				return value;
		}
		return params.defaultFor(i);
	}

	public static boolean isBlock(Object x) {
		return x instanceof SuCallable && ((SuCallable) x).isBlock;
	}

	// support methods for generated code --------------------------------------

	public final Object superInvoke(Object self, String member, Object... args) {
		return myClass.superInvoke(self, member, args);
	}

	public final Object[] massage(Object[] args) {
		return Args.massage(params, args);
	}

	//TODO change to use context

	public final Object contextGet(String name) {
		return Globals.get(name);
	}

	public final Object invoke(String name, Object... args) {
		return ((SuValue) Globals.get(name)).call(args);
	}
	public final Object invoke0(String name) {
		return ((SuValue) Globals.get(name)).call0();
	}
	public final Object invoke1(String name, Object a) {
		return ((SuValue) Globals.get(name)).call1(a);
	}
	public final Object invoke2(String name, Object a, Object b) {
		return ((SuValue) Globals.get(name)).call2(a, b);
	}
	public final Object invoke3(String name, Object a, Object b, Object c) {
		return ((SuValue) Globals.get(name)).call3(a, b, c);
	}
	public final Object invoke4(String name, Object a, Object b,
			Object c, Object d) {
		return ((SuValue) Globals.get(name)).call4(a, b, c, d);
	}

	//--------------------------------------------------------------------------

	@Override
	public String toString() {
		return super.typeName().replace(AstCompile.METHOD_SEPARATOR, '.');
	}

}
