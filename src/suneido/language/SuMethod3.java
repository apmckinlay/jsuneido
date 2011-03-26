/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

public abstract class SuMethod3 extends SuMethod {
	{ params = new FunctionSpec("a", "b", "c"); }

	@Override
	public Object eval(Object self, Object... args) {
		args = Args.massage(params, args);
		return eval3(self, args[0], args[1], args[2]);
	}

	@Override
	public Object eval0(Object self) {
		return eval3(self, defaultFor(0), defaultFor(1), defaultFor(2));
	}

	@Override
	public Object eval1(Object self, Object a) {
		return eval3(self, a, defaultFor(1), defaultFor(2));
	}

	@Override
	public Object eval2(Object self, Object a, Object b) {
		return eval3(self, a, b, defaultFor(2));
	}

	@Override
	public abstract Object eval3(Object self, Object a, Object b, Object c);

}
