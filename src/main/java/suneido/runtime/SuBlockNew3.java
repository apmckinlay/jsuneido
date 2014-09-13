/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

/**
 * A block taking three parameters.
 *
 * @author Victor Schappert
 * @since 20140912
 * @see SuFunction3
 * @see SuClosure
 */
public abstract class SuBlockNew3 extends SuBlockNew {

	{ params = new FunctionSpec("a", "b", "c"); }

	@Override
	public Object call(Object... args) {
		args = Args.massage(params, args);
		return call3(args[0], args[1], args[2]);
	}

	@Override
	public Object call0() {
		return call3(fillin(0), fillin(1), fillin(2));
	}

	@Override
	public Object call1(Object a) {
		return call3(a, fillin(1), fillin(2));
	}

	@Override
	public Object call2(Object a, Object b) {
		return call3(a, b, fillin(2));
	}

	@Override
	public abstract Object call3(Object a, Object b, Object c);
}
