package suneido.language.builtin;

import static suneido.util.Util.array;
import suneido.language.*;

public class Trace extends SuFunction {

	private static final FunctionSpec fs =
			new FunctionSpec(array("flags", "block"), false);

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		int flags = Ops.toInt(args[0]);
		suneido.Trace.flags = flags;
		if (args[1] != Boolean.FALSE)
			return Ops.call(args[1]);
		return suneido.Trace.flags;
	}

}
