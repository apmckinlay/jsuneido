/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;


/**
 * Class methods (and stand-alone functions that reference "this")
 * derive from SuMethod and define eval<p>
 * Other stand-alone functions derive from {@link SuFunction) and define call<p>
 * For simple args {@link SuMethod0} ... {@link SuMethod9} are used
 * @see SuBoundMethod
 */
public abstract class SuMethod extends SuCallable {

	@Override
	public String typeName() {
		return "Method";
	}

	@Override
	public abstract Object eval(Object self, Object...args);

	@Override
	public Object call(Object... args) {
		return eval(this, args);
	}
	@Override
	public Object call0() {
		return eval0(this);
	}
	@Override
	public Object call1(Object a) {
		return eval1(this, a);
	}
	@Override
	public Object call2(Object a, Object b) {
		return eval2(this, a, b);
	}
	@Override
	public Object call3(Object a, Object b, Object c) {
		return eval3(this, a, b, c);
	}
	@Override
	public Object call4(Object a, Object b, Object c, Object d) {
		return eval4(this, a, b, c, d);
	}

}
