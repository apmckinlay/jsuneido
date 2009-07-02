package suneido.language.builtin;

import suneido.language.*;

public class Synchronized extends SuFunction {

	private static final FunctionSpec fs = new FunctionSpec("block");

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		return Ops.call(args[0]);
		// TODO Synchronized, which is really atomic i.e. stop the world
	}

}
