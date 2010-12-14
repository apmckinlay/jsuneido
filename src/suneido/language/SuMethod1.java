/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

public abstract class SuMethod1 extends SuMethod {
	{ params = new FunctionSpec("value"); }

	@Override
	public Object eval(Object self, Object... args) {
		args = Args.massage(params, args);
		return eval1(self, args[0]);
	}

	@Override
	public Object eval0(Object self) {
		return eval1(self, defaultFor(0));
	}

	@Override
	public abstract Object eval1(Object self, Object a);

}
