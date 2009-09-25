package suneido.language.builtin;

import suneido.language.*;

public class Gt extends BuiltinFunction {

	@Override
	public Boolean call(Object... args) {
		Args.massage(FunctionSpec.value2, args);
		return Ops.gt(args[0], args[1]);
	}

}
