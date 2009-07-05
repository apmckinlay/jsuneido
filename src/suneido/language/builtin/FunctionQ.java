package suneido.language.builtin;

import suneido.language.*;

public class FunctionQ extends SuFunction {

	@Override
	public Object call(Object... args) {
		args = Args.massage(FunctionSpec.value, args);
		return args[0] instanceof SuFunction
				|| args[0] instanceof SuMethod
				|| args[0] instanceof SuBlock;
	}

}
