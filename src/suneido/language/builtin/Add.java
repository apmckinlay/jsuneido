package suneido.language.builtin;

import suneido.language.*;

public class Add extends SuFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.value2, args);
		return Ops.add(args[0], args[1]);
	}

}
