/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

public abstract class BuiltinMethod0 extends SuMethod {

	@Override
	public Object eval(Object self, Object... args) {
		Args.massage(FunctionSpec.noParams, args);
		return eval0(self);
	}

	@Override
	public abstract Object eval0(Object self);

}
