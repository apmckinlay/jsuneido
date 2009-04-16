package suneido.language;

import suneido.SuValue;

abstract public class SuCallable extends SuValue {
	protected FunctionSpec[] params;
	protected Object[][] constants;

	@Override
	public String toString() {
		return typeName();
	}

	@Override
	public boolean equals(Object other) {
		return this == other; // identity
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}

}
