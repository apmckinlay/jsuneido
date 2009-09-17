package suneido.language;

import suneido.SuException;
import suneido.SuValue;

abstract public class SuCallable extends SuValue {
	protected FunctionSpec[] params;
	protected Object[][] constants;
	protected Object self = null; // used within call methods, set by eval

	@Override
	public Object eval(Object self, Object... args) {
		SuCallable fn2;
		try {
			fn2 = getClass().newInstance();
		} catch (InstantiationException e) {
			throw new SuException("object.Eval bad function");
		} catch (IllegalAccessException e) {
			throw new SuException("object.Eval bad function");
		}
		fn2.self = self;
		fn2.params = params;
		fn2.constants = constants;
		return fn2.call(args);
	}

}
