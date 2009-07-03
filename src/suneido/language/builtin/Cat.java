package suneido.language.builtin;

import suneido.language.*;

public class Cat extends SuFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.value2, args);
		return Ops.cat(args[0], args[1]);
	}

}
