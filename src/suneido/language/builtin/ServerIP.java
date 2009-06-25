package suneido.language.builtin;

import suneido.language.*;

public class ServerIP extends SuFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.noParams, args);
		return ""; // TODO ServerIP
	}

}

