/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.runtime;

/**
 * A bound instance of a block.
 * Points to a generated sub-class of SuCallable
 * which defines an eval method.
 */
public class SuBlock extends SuCallable {
	protected final SuCallable block;
	protected final BlockSpec bspec;
	protected final Object self;
	protected final Object[] locals;

	public SuBlock(Object block, Object self, Object[] locals) {
		this.block = (SuCallable) block;
		bspec = (BlockSpec) this.block.params;
		this.self = self;
		this.locals = locals;
		isBlock = true;
	}

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
		for (int i = 0; i < bspec.params.length; ++i)
			locals[bspec.iparams + i] = args[i];
		return block.eval(newSelf, locals);
	}

	@Override
	public String toString() {
		return "aBlock";
	}

	@Override
	public String typeName() {
		return "Block";
	}

}
