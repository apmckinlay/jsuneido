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
	}

	// NOTE merging args into locals is not thread safe
	// i.e. can't call a block from more than one thread

	@Override
	public Object call(Object... args) {
		return call(self, args);
	}

	@Override
	public Object eval(Object newSelf, Object... args) {
		return call(newSelf, args);
	}

	private Object call(Object self, Object... args) {
		BlockSpec bspec = (BlockSpec) block.params;
		args = Args.massage(bspec, args);
		// merge args into locals
		for (int i = 0; i < bspec.nparams; ++i)
			locals[bspec.iparams + i] = args[i];
		return block.eval(self, locals);
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
