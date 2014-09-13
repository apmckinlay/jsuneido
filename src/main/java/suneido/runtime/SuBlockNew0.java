/* Copyright 2014 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

/**
 * A block taking no parameters.
 *
 * @author Victor Schappert
 * @since 20140912
 * @see SuFunction0
 * @see SuClosure
 */
public abstract class SuBlockNew0 extends SuBlockNew {

	{ params = FunctionSpec.NO_PARAMS; }

	@Override
	public Object call(Object... args) {
		Args.massage(params, args);
		return call0();
	}

	@Override
	public abstract Object call0();
}
