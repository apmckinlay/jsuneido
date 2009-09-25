package suneido.language.builtin;

import suneido.language.*;

public class NumberQ extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		args = Args.massage(FunctionSpec.value, args);
		return args[0] instanceof Number;
	}

}
