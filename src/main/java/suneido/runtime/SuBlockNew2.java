/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

/**
 * A block taking two parameters.
 *
 * @author Victor Schappert
 * @since 20140912
 * @see SuFunction2
 * @see SuClosure
 */
public abstract class SuBlockNew2 extends SuBlockNew {

	{ params = FunctionSpec.VALUE2; }

	@Override
	public Object call(Object... args) {
		args = Args.massage(params, args);
		return call2(args[0], args[1]);
	}

	@Override
	public Object call0() {
		return call2(fillin(0), fillin(1));
	}

	@Override
	public Object call1(Object a) {
		return call2(a, fillin(1));
	}

	@Override
	public abstract Object call2(Object a, Object b);
}
