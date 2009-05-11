package suneido.language.builtin;

import suneido.language.*;

public class Display extends SuFunction {

	private static final FunctionSpec fs = new FunctionSpec("value");

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		return Ops.display(args[0]);
	}

}
