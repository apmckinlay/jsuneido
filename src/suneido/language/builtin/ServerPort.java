package suneido.language.builtin;

import suneido.language.*;

public class ServerPort extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.noParams, args);
		return ""; // TODO ServerPort
	}

}
