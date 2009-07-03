package suneido.language.builtin;

import suneido.language.*;

public class NoMatch extends SuFunction {

	@Override
	public Boolean call(Object... args) {
		Args.massage(FunctionSpec.value2, args);
		return Ops.matchnot(args[0], args[1]);
	}

}
