/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

import com.google.common.base.Ascii;

import suneido.runtime.builtin.SuFile;

/**
 * The base class for built-in classes such {@link Adler32} and {@link SuFile}
 * Inherits method handling from {@link BuiltinMethods}.
 * Provides typeName() and toString().
 * Derived classes must define a newInstance method to implement "new Xyz(...)"
 * and may optionally define a call method.
 * NOTE: This is the base for classes, not instances.
 */
public abstract class BuiltinClass extends BuiltinMethods {

	protected BuiltinClass(Class<?> clazz) {
		this(clazz.getSimpleName(), clazz, null, FunctionSpec.NO_PARAMS);
	}

	protected BuiltinClass(String className) {
		this(className, FunctionSpec.NO_PARAMS);
	}

	protected BuiltinClass(String className, FunctionSpec newInstanceParams) {
		this(className, null, null, newInstanceParams);
	}

	protected BuiltinClass(String className, Class<?> c, String userDefined,
			FunctionSpec newInstanceParams) {
		super(Ascii.toLowerCase(className), c, userDefined);
		newInstance = new SuBuiltinMethod(className + ".<new>",
				newInstanceParams) {
			@Override
			public Object eval(Object self, Object... args) {
				return newInstance(args);
			}
		};
	}

	@Override
	public Object call(Object... args) {
		return newInstance(args);
	}

	@Override
	public Object get(Object member) {
		return super.lookup(Ops.toStr(member));
	}

	@Override
	public SuCallable lookup(String method) {
		if ("<new>".equals(method))
			return newInstance;
		// TODO Base, Base?, etc.
		return super.lookup(method);
	}

	private final SuCallable newInstance;

	protected abstract Object newInstance(Object... args);

	@Override
	public String toString() {
		return super.typeName() + " /* builtin class */";
	}

	@Override
	public String typeName() {
		return "BuiltinClass";
	}

}
