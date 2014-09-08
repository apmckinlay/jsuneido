/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

public abstract class SuMethod3 extends SuMethod {
	{ params = new FunctionSpec("a", "b", "c"); }

	@Override
	public Object eval(Object self, Object... args) {
		args = Args.massage(params, args);
		return eval3(self, args[0], args[1], args[2]);
	}

	@Override
	public Object eval0(Object self) {
		return eval3(self, fillin(0), fillin(1), fillin(2));
	}

	@Override
	public Object eval1(Object self, Object a) {
		return eval3(self, a, fillin(1), fillin(2));
	}

	@Override
	public Object eval2(Object self, Object a, Object b) {
		return eval3(self, a, b, fillin(2));
	}

	@Override
	public abstract Object eval3(Object self, Object a, Object b, Object c);

}
