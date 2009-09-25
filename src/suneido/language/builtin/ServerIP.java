package suneido.language.builtin;

import suneido.language.*;

public class ServerIP extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.noParams, args);
		return ""; // TODO ServerIP
	}

}

