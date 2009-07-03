package suneido.language.builtin;

import suneido.language.*;

public class Div extends SuFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.value2, args);
		return Ops.div(args[0], args[1]);
	}

}
