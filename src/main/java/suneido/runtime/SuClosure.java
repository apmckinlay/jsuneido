/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

/**
 * <p>
 * A block in which some of the local variables are "upvalues" that belong to
 * a higher lexical scope. Wraps a block, which is a subclass of either 
 * {@link SuCallBase} (if no {@code this} reference) or {@link SuEvalBase}
 * (if there's a {@code this} reference).
 * </p>
 *
 * @author Andrew McKinlay
 */
public class SuClosure extends SuWrappingCallable {
	protected final BlockSpec bspec;
	protected final Object self;
	protected final Object[] locals;

	public SuClosure(Object block, Object self, Object[] locals) {
		super((SuCallable)block);
		callableType = CallableType.CLOSURE;
		bspec = (BlockSpec) this.wrapped.params;
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
		return wrapped.eval(newSelf, locals);
	}
}
