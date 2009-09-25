package suneido.language.builtin;

import suneido.language.*;

public class Display extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		args = Args.massage(FunctionSpec.value, args);
		return Ops.display(args[0]);
	}

}
