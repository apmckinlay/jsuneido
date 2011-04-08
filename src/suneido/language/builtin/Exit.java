package suneido.language.builtin;

import static suneido.language.Ops.toInt;
import static suneido.util.Util.array;
import suneido.language.*;

public class Exit extends SuFunction {

	public static final FunctionSpec statusFS = new FunctionSpec(array("status"), 0);

	@Override
	public Object call(Object... args) {
		args = Args.massage(statusFS, args);
		System.exit(toInt(args[0]));
		return null;
	}

}
