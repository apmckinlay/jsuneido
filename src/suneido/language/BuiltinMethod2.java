/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

public abstract class BuiltinMethod2 extends SuMethod {
	{ params = new FunctionSpec("value1", "value2"); }

	@Override
	public Object eval(Object self, Object... args) {
		args = Args.massage(params, args);
		return eval2(self, args[0], args[1]);
	}

	@Override
	public abstract Object eval2(Object self, Object a, Object b);

}
