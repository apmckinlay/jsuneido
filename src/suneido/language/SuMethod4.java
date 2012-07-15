/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

public abstract class SuMethod4 extends SuMethod {
	{ params = new FunctionSpec("a", "b", "c", "d"); }

	@Override
	public Object eval(Object self, Object... args) {
		args = Args.massage(params, args);
		return eval4(self, args[0], args[1], args[2], args[3]);
	}

	@Override
	public Object eval0(Object self) {
		return eval4(self, fillin(0), fillin(1), fillin(2), fillin(3));
	}

	@Override
	public Object eval1(Object self, Object a) {
		return eval4(self, a, fillin(1), fillin(2), fillin(3));
	}

	@Override
	public Object eval2(Object self, Object a, Object b) {
		return eval4(self, a, b, fillin(2), fillin(3));
	}

	@Override
	public Object eval3(Object self, Object a, Object b, Object c) {
		return eval4(self, a, b, c, fillin(3));
	}

	@Override
	public abstract Object eval4(Object self, Object a, Object b, Object c, Object d);

}
