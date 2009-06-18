package suneido.language.builtin;

import suneido.language.*;

public class ServerIP extends SuFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.noParams, args);
		return "127.0.0.1"; // TODO ServerIP
	}

}

