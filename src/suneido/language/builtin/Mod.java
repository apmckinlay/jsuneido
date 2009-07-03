package suneido.language.builtin;

import suneido.language.*;

public class Mod extends SuFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.value2, args);
		return Ops.mod(args[0], args[1]);
	}

}
