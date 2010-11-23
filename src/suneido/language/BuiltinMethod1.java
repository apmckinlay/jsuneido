/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

public abstract class BuiltinMethod1 extends SuMethod {

	@Override
	public Object eval(Object self, Object... args) {
		args = Args.massage(params, args);
		return eval1(self, args[0]);
	}

	@Override
	public abstract Object eval1(Object self, Object a);

}
