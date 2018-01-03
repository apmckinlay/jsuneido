/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import suneido.SuValue;
import suneido.compiler.Disassembler;

/**
 * <p>
 * A Suneido value that is callable by a Suneido language call expression.
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
	 */
	public final CallableType callableType() {
		return callableType;
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

	public byte[] byteCode() {
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
			return "eval".equals(name)
					|| name.endsWith(CallableType.FUNCTION.compilerNameSuffix());
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
		return x instanceof SuCallable && ((SuCallable) x).callableType.isBlock();
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
	public String internalName() {
		return name;
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
	public SuValue lookup(String method) {
		switch (method) {
		case "Params":
			return Params;
		case "Source":
			return Source;
		case "Disasm":
			return Disasm;
		}
		return super.lookup(method);
	}

	private static final SuValue Params =
			new SuBuiltinMethod0("function.Params") {
		@Override
		public Object eval0(Object self) {
			FunctionSpec p = ((SuCallable) self).params;
			return p == null ? "(...)" : p.params();
		}
	};

	private static final SuValue Source =
			new SuBuiltinMethod0("function.Source") {
		@Override
		public Object eval0(Object self) {
			return ((SuCallable) self).sourceCode();
		}
	};

	private static final SuValue Disasm =
			new SuBuiltinMethod0("function.Disasm") {
		@Override
		public Object eval0(Object self) {
			return Disassembler.disassemble((SuCallable)self);
		}
	};
}
