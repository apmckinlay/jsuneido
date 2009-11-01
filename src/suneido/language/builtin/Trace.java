package suneido.language.builtin;

import static suneido.util.Util.array;
import suneido.language.*;

public class Trace extends BuiltinFunction {

	private static final FunctionSpec fs =
			new FunctionSpec(array("flags", "block"), false);

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		// int n = Ops.toInt(args[0]);
		// TODO Trace
		if (args[1] != Boolean.FALSE)
			return Ops.call(args[1]);
		return null;
	}

}
