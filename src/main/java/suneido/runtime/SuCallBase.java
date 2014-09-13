/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

/**
 * <p>
 * Base class for standalone functions and blocks derive, as long as they do not
 * refer to "{@code this}". Callable entities deriving from this class
 * define {@code call}. 
 * </p>
 *
 * <p>
 * Functions that reference "{@code this}" derive from {@link SuEvalBase} and
 * define {@code eval}.
 * </p>
 * 
 * <p>
 * For simple args {@link SuCallBase0} ... {@link SuCallBase4} are used.
 * </p>
 *
 * @author Andrew McKinlay, Victor Schappert
 */
public abstract class SuCallBase extends SuCallable {

	//
	// ANCESTOR CLASS: SuValue
	//

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
