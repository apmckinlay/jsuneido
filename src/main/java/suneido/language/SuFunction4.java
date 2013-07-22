/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

public abstract class SuFunction4 extends SuFunction {

	{ params = new FunctionSpec("a", "b", "c", "d"); }

	@Override
	public Object call(Object... args) {
		args = Args.massage(params, args);
		return call4(args[0], args[1], args[2], args[3]);
	}

	@Override
	public Object call0() {
		return call4(fillin(0), fillin(1), fillin(2), fillin(3));
	}

	@Override
	public Object call1(Object a) {
		return call4(a, fillin(1), fillin(2), fillin(3));
	}

	@Override
	public Object call2(Object a, Object b) {
		return call4(a, b, fillin(2), fillin(3));
	}

	@Override
	public Object call3(Object a, Object b, Object c) {
		return call4(a, b, c, fillin(3));
	}

	@Override
	public abstract Object call4(Object a, Object b, Object c, Object d);

}
