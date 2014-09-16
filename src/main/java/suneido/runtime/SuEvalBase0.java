/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

public abstract class SuEvalBase0 extends SuEvalBase {
	{ params = FunctionSpec.NO_PARAMS; }

	@Override
	public Object eval(Object self, Object... args) {
		Args.massage(params, args);
		return eval0(self);
	}

	@Override
	public abstract Object eval0(Object self);

}
