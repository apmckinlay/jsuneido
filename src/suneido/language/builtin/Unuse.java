package suneido.language.builtin;

import suneido.language.*;

public class Unuse extends SuFunction {

	private static final FunctionSpec unuseFS = new FunctionSpec("library");

	@Override
	public Object call(Object... args) {
		args = Args.massage(unuseFS, args);
		// TODO unuse
		return false;
	}

}
