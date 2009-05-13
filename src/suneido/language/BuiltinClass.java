package suneido.language;

import suneido.SuValue;

public abstract class BuiltinClass extends SuValue {

	@Override
	public Object call(Object... args) {
		return newInstance(args);
	}

	@Override
	public Object invoke(Object self, String method, Object... args) {
		if (method == "<new>")
			return newInstance(args);
		return super.invoke(self, method, args);
	}

	abstract public Object newInstance(Object[] args);

}
