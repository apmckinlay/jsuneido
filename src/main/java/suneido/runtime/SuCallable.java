/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import suneido.SuValue;
import suneido.compiler.AstCompile;

/**
 * Abstract base class for {@link SuBlock}, {@link SuFunction},
 * {@link SuMethod}, and {@link SuBoundMethod}
 */
public abstract class SuCallable extends SuValue {
	private String library;
	protected String name;
	protected SuClass myClass;
	protected FunctionSpec params;
	protected ContextLayered context;
	protected boolean isBlock = false;

	/**
	 * Called by the compiler to complete construction of a newly compiled
	 * SuCallable (<i>eg</i> a class method).
	 *
	 * @since 20140829
	 * @param myClass Callable's class if applicable
	 * @param params Callable's function spec
	 * @param context Context callable belongs to
	 * @param isBlock Whether callable is a block
	 * @return this
	 */
	public final SuCallable finishInit(SuClass myClass, FunctionSpec params,
			ContextLayered context, boolean isBlock) {
		this.myClass = myClass;
		this.params = params;
		this.context = context;
		this.isBlock = isBlock;
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

	@Override
	public SuValue lookup(String methodName) {
		if (methodName == "Params") // method names are interned so ok to use ==
			return Params;
		return super.lookup(methodName);
	}

	private static SuValue Params = new SuMethod0() {
		@Override
		public Object eval0(Object self) {
			FunctionSpec p = ((SuCallable) self).params;
			return p == null ? "(...)" : p.params();
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

	public SuClass suClass() {
		return myClass;
	}

	protected boolean hasLibrary() {
		return null != library && !library.isEmpty();
	}

	protected String getLibrary() {
		return library;
	}

	// support methods for generated code  for calling globals -----------------

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

	//--------------------------------------------------------------------------

	@Override
	public String valueName() {
		return null == name ? "" : name;
	}

	@Override
	public String display() {
		if (isBlock)
			return "/* block */";
		String type = super.typeName();
		if (type.endsWith("$f"))
			return "/* function */";
		StringBuilder sb = new StringBuilder();
		sb.append(type.replace(AstCompile.METHOD_SEPARATOR, '.'));
		if (hasLibrary())
			sb.append(" /* ").append(getLibrary()).append(" ")
					.append(typeName().toLowerCase()).append(" */");
		return sb.toString();
	}

}
