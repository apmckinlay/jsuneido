package suneido.language.builtin;

import suneido.language.*;

public class Mul extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.value2, args);
		return Ops.mul(args[0], args[1]);
	}

}
