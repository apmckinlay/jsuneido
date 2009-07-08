package suneido.language;

import suneido.SuException;
import suneido.SuValue;

public class SuBlock extends SuValue {
	private final Object instance;
	private final BlockSpec bspec;
	private final Object[] locals;

	public SuBlock(Object instance, FunctionSpec bspec, Object[] locals) {
		this.instance = instance;
		this.bspec = (BlockSpec) bspec;
		this.locals = locals;
	}

	// NOTE merging args into locals is not thread safe
	// i.e. can't call a block from more than one thread

	@Override
	public Object call(Object... args) {
		args = Args.massage(bspec, args);
		// merge args into locals
		for (int i = 0; i < bspec.nparams; ++i)
			locals[bspec.iparams + i] = args[i];
		return Ops.invoke(instance, bspec.name, locals);
	}

	@Override
	public Object eval(Object self, Object[] args) {
		if (!(instance instanceof SuValue))
			throw new SuException("Eval requires Suneido value");
		args = Args.massage(bspec, args);
		// merge args into locals
		for (int i = 0; i < bspec.nparams; ++i)
			locals[bspec.iparams + i] = args[i];
		return ((SuValue) instance).invoke(self, bspec.name, locals);
	}

	@Override
	public String toString() {
		return (instance == null ? "null" : instance.toString())
				+ "."
				+ bspec.name;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (other instanceof SuBlock) {
			SuBlock that = (SuBlock) other;
			return instance == that.instance
					&& bspec == that.bspec
					&& locals == that.locals;
		}
		return false;
	}

	/** as recommended by Effective Java */
	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + instance.hashCode();
		result = 31 * result + bspec.hashCode();
		result = 31 * result + locals.hashCode();
		return result;
	}

	@Override
	public String typeName() {
		return "Block";
	}

}
