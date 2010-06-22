package suneido.language;

import suneido.SuException;
import suneido.SuValue;

import com.google.common.base.Objects;

public class SuBlock extends SuValue {
	private final Object home; // defining class
	private final Object self;
	private final BlockSpec bspec;
	private final Object[] locals;

	public SuBlock(Object home, Object self, FunctionSpec bspec, Object[] locals) {
		this.home = home;
		this.self = self;
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
		return ((SuValue) home).invoke(self, bspec.name, locals);
	}

	@Override
	public Object eval(Object newSelf, Object... args) {
		if (!(home instanceof SuValue))
			throw new SuException("Eval requires Suneido value");
		args = Args.massage(bspec, args);
		// merge args into locals
		for (int i = 0; i < bspec.nparams; ++i)
			locals[bspec.iparams + i] = args[i];
		return ((SuValue) home).invoke(newSelf, bspec.name, locals);
	}

	@Override
	public String toString() {
		return "block:" + (home == null ? "null" : home.toString())
				+ "."
				+ bspec.name;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (! (other instanceof SuBlock))
			return false;
		SuBlock that = (SuBlock) other;
		return home == that.home
				&& bspec == that.bspec
				&& locals == that.locals;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(home, bspec, locals);
	}

	@Override
	public String typeName() {
		return "Block";
	}

}
