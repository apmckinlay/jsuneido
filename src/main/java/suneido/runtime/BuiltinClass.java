/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

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

	protected BuiltinClass() {
	}

	protected BuiltinClass(Class<?> c) {
		super(c);
	}
	protected BuiltinClass(Class<?> c, String userDefined) {
		super(c.getSimpleName().toLowerCase(), c, userDefined);
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
		if (method == "<new>")
			return newInstance;
		// TODO Base, Base?, etc.
		return super.lookup(method);
	}

	private final SuCallable newInstance = new SuMethod() {
		@Override
		public Object eval(Object self, Object... args) {
			return newInstance(args);
		}
	};

	protected abstract Object newInstance(Object... args);

	@Override
	public String typeName() {
		return getClass().getName().replace(Builtin.PACKAGE_NAME + '.', "");
	}

	@Override
	public String toString() {
		return typeName() + " /* builtin class */";
	}

}
