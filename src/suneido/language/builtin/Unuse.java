package suneido.language.builtin;

import suneido.language.*;

public class Unuse extends BuiltinFunction {

	private static final FunctionSpec unuseFS = new FunctionSpec("library");

	@Override
	public Object call(Object... args) {
		args = Args.massage(unuseFS, args);
		return suneido.language.Library.unuse(Ops.toStr(args[0]));
	}

}
