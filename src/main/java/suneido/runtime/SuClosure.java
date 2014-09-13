/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

/**
 * <p>
 * A block in which some of the local variables are "upvalues" that belong to
 * a higher lexical scope. Wraps an {@link SuBlockNew}.
 * </p>
 *
 * @author Andrew McKinlay
 */
public class SuClosure extends SuCallable {
	protected final SuCallable block;
	protected final BlockSpec bspec;
	protected final Object self;
	protected final Object[] locals;

	public SuClosure(Object block, Object self, Object[] locals) {
		callableType = CallableType.CLOSURE;
		this.block = (SuCallable) block;
		bspec = (BlockSpec) this.block.params;
		this.self = self;
		this.locals = locals;
	}

	//
	// ANCESTOR CLASS: SuValue
	//

	@Override
	public Object call(Object... args) {
		return eval(self, args);
	}

	// NOTE merging args into locals is not thread safe
	// i.e. can't call a block from more than one thread
	// also not reentrant - can't recurse
	@Override
	public Object eval(Object newSelf, Object... args) {
		args = Args.massage(bspec, args);
		// merge args into locals
		for (int i = 0; i < bspec.paramNames.length; ++i)
			locals[bspec.iparams + i] = args[i];
		return block.eval(newSelf, locals);
	}
}
