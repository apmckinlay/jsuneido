package suneido.language.builtin;

import suneido.language.*;

public class Sub extends SuFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.value2, args);
		return Ops.sub(args[0], args[1]);
	}

}
