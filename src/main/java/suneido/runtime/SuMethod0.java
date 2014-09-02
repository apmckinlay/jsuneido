/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

public abstract class SuMethod0 extends SuMethod {
	{ params = FunctionSpec.noParams; }

	@Override
	public Object eval(Object self, Object... args) {
		Args.massage(params, args);
		return eval0(self);
	}

	@Override
	public abstract Object eval0(Object self);

}
