package suneido.language;

/**
 * A bound instance of a block.
 * Points to a generated sub-class of SuCallable
 * which defines an eval method.
 */
public final class SuBlock extends SuCallable {
	private final SuCallable block;
	private final Object self;
	private final Object[] locals;

	public SuBlock(Object block, Object self, Object[] locals) {
		this.block = (SuCallable) block;
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
		BlockSpec bspec = (BlockSpec) block.params;
		args = Args.massage(bspec, args);
		// merge args into locals
		for (int i = 0; i < bspec.nparams; ++i)
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
