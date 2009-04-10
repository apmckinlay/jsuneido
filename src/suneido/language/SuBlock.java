package suneido.language;

import static suneido.language.SuClass.massage;
import suneido.SuValue;

public class SuBlock  extends SuValue {
	private final Object instance;
	private final FunctionSpec fspec;
	private final Object[] locals;

	public SuBlock(Object instance, FunctionSpec fspec, Object[] locals) {
		this.instance = instance;
		this.fspec = fspec;
		this.locals = locals;
	}

	@Override
	public Object invoke(String method, Object... args) {
		if (method == "call") {
			args = massage(fspec, args);
			// TODO merge args into locals
			return Ops.invoke(instance, fspec.name, locals);
		} else
			throw unknown_method(method);
	}

	@Override
	public String toString() {
		return (instance == null ? "null" : instance.toString())
				+ "." + fspec.name;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (other instanceof SuBlock) {
			SuBlock that = (SuBlock) other;
			return instance == that.instance
					&& fspec == that.fspec
					&& locals == that.locals;
		}
		return false;
	}

	/** as recommended by Effective Java */
	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + instance.hashCode();
		result = 31 * result + fspec.hashCode();
		result = 31 * result + locals.hashCode();
		return result;
	}



}
