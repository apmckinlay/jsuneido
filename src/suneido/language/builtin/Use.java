package suneido.language.builtin;

import suneido.language.*;

public class Use extends SuFunction {

	private static final FunctionSpec useFS = new FunctionSpec("library");

	@Override
	public Object call(Object... args) {
		args = Args.massage(useFS, args);
		return suneido.language.Libraries.use(Ops.toStr(args[0]));
	}

}
