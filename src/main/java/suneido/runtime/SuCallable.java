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
	private String library;
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

	/**
	 * Called by the compiler to complete construction of a newly compiled
	 * SuCallable.
	 *
	 * @since 20140829
	 * @param myClass
	 *            Callable's class if applicable
	 * @param params
	 *            Callable's function spec
	 * @param context
	 *            Context callable belongs to
	 * @param callableType
	 *            What kind of callable this is, <i>eg</i> function
	 * @return this
	 */
	public final SuCallable finishInit(SuClass myClass, FunctionSpec params,
			ContextLayered context, CallableType callableType) {
		this.myClass = myClass;
		this.params = params;
		this.context = context;
		this.callableType = callableType;
		return this;
	}

	/**
	 * Called by AstCompile to set the library and name
	 * that the source code came from.
	 */
	public final SuCallable setSource(String library, String name) {
		this.library = library;
		this.name = name;
		return this;
	}

	public SuClass suClass() {
		return myClass;
	}

	public FunctionSpec getParams() {
		return params;
	}

	//
	// INTERNALS
	//

	/**
	 * Supply missing argument from dynamic implicit or default
	 * This is also done by {@link Args} applyDefaults and dynamicImplicits
	 */
	protected Object fillin(int i) {
		assert params != null : "" + this + " has no params";
		if (params.isDynParam(params.paramNames[i])) {
			Object value = Dynamic.getOrNull("_" + params.paramNames[i]);
			if (value != null)
				return value;
		}
		return params.defaultFor(i);
	}

	protected StringBuilder appendName(StringBuilder sb) {
		if (null != name && !name.isEmpty()) {
			sb.append(name).append(' ');
		}
		return sb;
	}

	protected StringBuilder appendLibrary(StringBuilder sb) {
		if (null != library && !library.isEmpty()) {
			sb.append(library).append(' ');
		}
		return sb;
	}

	//--------------------------------------------------------------------------
	// support methods for generated code  for calling globals -----------------
	//--------------------------------------------------------------------------

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
	public final Object invoke4(int slot, Object a, Object b,
			Object c, Object d) {
		return ((SuValue) contextGet(slot)).call4(a, b, c, d);
	}
	
	public static boolean isBlock(Object x) {
		return x instanceof SuCallable && ((SuCallable) x).isBlock();
	}

	//--------------------------------------------------------------------------
	
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
		if ("Params".equals(methodName))
			return Params;
		return super.lookup(methodName);
	}

	private static SuValue Params = new SuEvalBase0() {
		@Override
		public Object eval0(Object self) {
			FunctionSpec p = ((SuCallable) self).params;
			return p == null ? "(...)" : p.params();
		}
	};
}
