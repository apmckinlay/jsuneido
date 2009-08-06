package suneido.language.builtin;

import suneido.language.*;

public class ObjectQ extends SuFunction {

	@Override
	public Object call(Object... args) {
		args = Args.massage(FunctionSpec.value, args);
		return args[0] instanceof SuInstance
				|| null != Ops.toContainer(args[0]);
	}

}
