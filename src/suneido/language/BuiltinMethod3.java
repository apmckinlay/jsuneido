/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

public abstract class BuiltinMethod3 extends SuMethod {
	{ params = new FunctionSpec("value1", "value2", "value3"); }

	@Override
	public Object eval(Object self, Object... args) {
		args = Args.massage(params, args);
		return eval3(self, args[0], args[1], args[2]);
	}

	@Override
	public abstract Object eval3(Object self, Object a, Object b, Object c);

}
