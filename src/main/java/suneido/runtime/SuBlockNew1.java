/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

/**
 * A block taking one parameter.
 *
 * @author Victor Schappert
 * @since 20140912
 * @see SuFunction1
 * @see SuClosure
 */
public abstract class SuBlockNew1 extends SuBlockNew {

	{ params = FunctionSpec.VALUE; }

	@Override
	public Object call(Object... args) {
		args = Args.massage(params, args);
		return call1(args[0]);
	}

	@Override
	public Object call0() {
		return call1(fillin(0));
	}

	@Override
	public abstract Object call1(Object a);
}
