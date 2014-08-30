/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

/**
 * <p>
 * Standalone functions derive from SuFunction and define {@code eval}.
 * </p>
 *
 * <p>
 * Functions that reference "this" derive from {@link SuMethod} and define
 * {@code eval}.
 * </p>
 * 
 * <p>
 * For simple args {@link SuFunction0} ... {@link SuFunction4} are used.
 * </p>
 */
public abstract class SuFunction extends SuCallable {

	@Override
	public String typeName() {
		return getClass().getName().startsWith(Builtin.PACKAGE_NAME)
			? "Builtin"
			: isBlock ? "Block" : "Function";
	}

	@Override
	public abstract Object call(Object... args);

	@Override
	public Object eval(Object self, Object... args) {
		return call(args);
	}
	@Override
	public Object eval0(Object self) {
		return call0();
	}
	@Override
	public Object eval1(Object self, Object a) {
		return call1(a);
	}
	@Override
	public Object eval2(Object self, Object a, Object b) {
		return call2(a, b);
	}
	@Override
	public Object eval3(Object self, Object a, Object b, Object c) {
		return call3(a, b, c);
	}
	@Override
	public Object eval4(Object self, Object a, Object b, Object c, Object d) {
		return call4(a, b, c, d);
	}

}
