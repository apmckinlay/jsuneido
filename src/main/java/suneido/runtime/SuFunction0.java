/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

public abstract class SuFunction0 extends SuFunction {
	{ params = FunctionSpec.noParams; }

	@Override
	public Object call(Object... args) {
		Args.massage(params, args);
		return call0();
	}

	@Override
	public abstract Object call0();

}