/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

/**
 * A "call"-based callable (standalone function or block) taking no parameters.
 *
 * @author Andrew McKinlay
 * @see SuEvalBase1
 */
public abstract class SuCallBase0 extends SuCallBase {
	{ params = FunctionSpec.NO_PARAMS; }

	@Override
	public Object call(Object... args) {
		Args.massage(params, args);
		return call0();
	}

	@Override
	public abstract Object call0();
}