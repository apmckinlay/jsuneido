package suneido.language.builtin;

import suneido.language.*;

public class StringQ extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		args = Args.massage(FunctionSpec.value, args);
		return Ops.isString(args[0]);
	}

}
