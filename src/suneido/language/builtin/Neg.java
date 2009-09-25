package suneido.language.builtin;

import suneido.language.*;

public class Neg extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.value, args);
		return Ops.uminus(args[0]);
	}

}
