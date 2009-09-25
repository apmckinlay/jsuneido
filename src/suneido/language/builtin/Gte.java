package suneido.language.builtin;

import suneido.language.*;

public class Gte extends BuiltinFunction {

	@Override
	public Boolean call(Object... args) {
		Args.massage(FunctionSpec.value2, args);
		return Ops.gte(args[0], args[1]);
	}

}
