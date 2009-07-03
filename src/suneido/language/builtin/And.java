package suneido.language.builtin;

import suneido.language.*;

public class And extends SuFunction {

	@Override
	public Boolean call(Object... args) {
		Args.massage(FunctionSpec.value2, args);
		return (Boolean) args[0] && (Boolean) args[1];
	}

}
