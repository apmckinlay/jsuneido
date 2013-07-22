/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

public abstract class SuMethod2 extends SuMethod {
	{ params = FunctionSpec.value2; }

	@Override
	public Object eval(Object self, Object... args) {
		args = Args.massage(params, args);
		return eval2(self, args[0], args[1]);
	}

	@Override
	public Object eval0(Object self) {
		return eval2(self, fillin(0), fillin(1));
	}

	@Override
	public Object eval1(Object self, Object a) {
		return eval2(self, a, fillin(1));
	}

	@Override
	public abstract Object eval2(Object self, Object a, Object b);

}
