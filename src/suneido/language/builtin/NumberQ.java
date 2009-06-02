package suneido.language.builtin;

import suneido.language.*;

public class NumberQ extends SuFunction {

	private static final FunctionSpec fs = new FunctionSpec("value");

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		return args[0] instanceof Number;
	}

}
