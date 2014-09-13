/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

/**
 * A "call"-based callable (standalone function or block) taking one parameter.
 *
 * @author Andrew McKinlay
 * @see SuEvalBase1
 */
public abstract class SuCallBase1 extends SuCallBase {

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
