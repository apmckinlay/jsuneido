/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import suneido.SuValue;

/**
 * <p>
 * A Suneido value that is callable by a Suneido language call expression.
 * </p>
 *
 * <p>
 * Examples:
 * <ul>
 * <li>a function : {@link SuCallBase} if it does not refer to {@code this}, or
 * {@link SuEvalBase} if it does</li>
 * <li>a method : {@link SuEvalBase} etc.</li>
 * <li>a plain block : {@link SuCallBase} if it does not refer to {@code this},
 * or {@link SuEvalBase} if it does</li>
 * <li>a closure : {@link SuClosure} etc.</li>
 * <li>a method which has been bound to an instance : {@link SuBoundMethod}</li>
 * <li>a built-in function : see {@link Builtin}</li>
 * <li>a built-in method : see {@link Builtin}</li>
 * </ul>
 * </p>
 *
 * @author Andrew McKinlay, Victor Schappert
 */
public class SuCallable extends SuValue {
	protected String name;
	protected SuClass myClass;
	protected FunctionSpec params;
	protected ContextLayered context;
	protected CallableType callableType;

	/**
	 * Returns the broad callable category this callable belongs to
	 *
	 * @return Callable type
	 * @see #isBlock()
	 */
	public final CallableType callableType() {
		return callableType;
	}

	/**
	 * Indicates whether a callable is a Suneido block
	 *
	 * @return True iff "this" is block
	 * @see #callableType()
	 */
	public final boolean isBlock() {
		final CallableType c = callableType();
		return CallableType.BLOCK == c || CallableType.CLOSURE == c;
	}

	public SuClass suClass() {
		return myClass;
	}

	public FunctionSpec getParams() {
		return params;
	}

	public String sourceCode() {
		return null;
	}

	//
	// INTERNALS
	//

	/**
	 * Supply missing argument from dynamic implicit or default This is also
	 * done by {@link Args} applyDefaults and dynamicImplicits
	 */
	protected final Object fillin(int i) {
		assert params != null : "" + this + " has no params";
		if (params.isDynParam(params.paramNames[i])) {
			Object value = Dynamic.getOrNull("_" + params.paramNames[i]);
			if (value != null)
				return value;
		}
		return params.defaultFor(i);
	}

	private boolean isAnonymous() {
		if (null == name || name.isEmpty()) {
			return true;
		}
		switch (callableType) {
		case BLOCK:
		case WRAPPED_BLOCK:
			return true;
		case FUNCTION:
			return name.endsWith(CallableType.FUNCTION.compilerNameSuffix());
		default:
			return false;
		}
	}

	protected final StringBuilder appendName(StringBuilder sb) {
		if (!isAnonymous()) {
			sb.append(name).append(' ');
		}
		return sb;
	}

	protected StringBuilder appendLibrary(StringBuilder sb) {
		return sb;
	}

	// --------------------------------------------------------------------------
	// support methods for generated code for calling globals -----------------
	// --------------------------------------------------------------------------

	public final Object superInvoke(Object self, String member, Object... args) {
		return myClass.superInvoke(self, member, args);
	}

	public final Object[] massage(Object[] args) {
		return Args.massage(params, args);
	}

	public final Object contextGet(int slot) {
		return context.get(slot);
	}

	public final Object invoke(int slot, Object... args) {
		return ((SuValue) contextGet(slot)).call(args);
	}

	public final Object invoke0(int slot) {
		return ((SuValue) contextGet(slot)).call0();
	}

	public final Object invoke1(int slot, Object a) {
		return ((SuValue) contextGet(slot)).call1(a);
	}

	public final Object invoke2(int slot, Object a, Object b) {
		return ((SuValue) contextGet(slot)).call2(a, b);
	}

	public final Object invoke3(int slot, Object a, Object b, Object c) {
		return ((SuValue) contextGet(slot)).call3(a, b, c);
	}

	public final Object invoke4(int slot, Object a, Object b, Object c, Object d) {
		return ((SuValue) contextGet(slot)).call4(a, b, c, d);
	}

	public static boolean isBlock(Object x) {
		return x instanceof SuCallable && ((SuCallable) x).isBlock();
	}

	// --------------------------------------------------------------------------

	//
	// ANCESTOR CLASS: SuValue
	//

	@Override
	public boolean isCallable() {
		return true;
	}

	@Override
	public String valueName() {
		return null == name ? "" : name;
	}

	@Override
	public String typeName() {
		return callableType().typeName();
	}

	@Override
	public String display() {
		StringBuilder sb = new StringBuilder(64);
		appendName(sb).append("/* ");
		return appendLibrary(sb).append(callableType().displayString())
				.append(" */").toString();
	}

	@Override
	public SuValue lookup(String methodName) {
		// WARNING: The reason this class is subclassing SuBuiltinMethod0 for
		// its built-ins instead of usign BuiltinMethods.methods() is
		// that I encountered JVM instability leading to random crashes
		// when the following conditions obtained:
		// 1) SuCallable is loaded by a native agent on VMInit
		// (the "jsdebug" agent does this when it is loaded on
		// JVM startup to provide Suneido debugging support)
		// 2) SuCallable has a private static member whose
		// initializer calls BuiltinMethods.methods(SuCallable.class, ...)
		// 3) Java 1.8.0_20 on Windows or Linux (didn't test other
		// versions)
		// From the little I could glean from the crash logs, there is
		// likely a bug in the JVM where class loading, agent loading,
		// and method handle initialization don't play well together.
		// I tried to reproduce the bug using a stripped down project
		// but wasn't able to do so... -- VCS @ 20140914
		if ("Params".equals(methodName))
			return Params;
		else if ("Source".equals(methodName))
			return Source;
		return super.lookup(methodName);
	}

	private static final SuValue Params = new SuBuiltinMethod0(
			"function.Params") {
		@Override
		public Object eval0(Object self) {
			FunctionSpec p = ((SuCallable) self).params;
			return p == null ? "(...)" : p.params();
		}
	};

	private static final SuValue Source = new SuBuiltinMethod0(
			"function.Source") {
		@Override
		public Object eval0(Object self) {
			return ((SuCallable) self).sourceCode();
		}
	};
}
