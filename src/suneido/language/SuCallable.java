package suneido.language;

import suneido.SuValue;

abstract public class SuCallable extends SuValue {

	public void setup(FunctionSpec[] params, Object[][] constants) {
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
