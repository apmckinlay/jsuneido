package suneido.language.builtin;

import suneido.language.*;

public class Not extends SuFunction {

	@Override
	public Boolean call(Object... args) {
		Args.massage(FunctionSpec.value, args);
		return Ops.not(args[0]);
	}

}
