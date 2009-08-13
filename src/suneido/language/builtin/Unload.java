package suneido.language.builtin;

import suneido.language.*;

public class Unload extends SuFunction {

	private static final FunctionSpec unloadFS = new FunctionSpec("name");

	@Override
	public Object call(Object... args) {
		args = Args.massage(unloadFS, args);
		Globals.unload(Ops.toStr(args[0]));
		return null;
	}

}
