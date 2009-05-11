package suneido.language.builtin;

import suneido.language.*;

public class FunctionQ extends SuFunction {

	private static final FunctionSpec fs = new FunctionSpec("value");

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		return args[0] instanceof SuFunction 
				|| args[0] instanceof SuMethod
				|| args[0] instanceof SuBlock;
	}

}
