package suneido.language.builtin;

import suneido.SuContainer;
import suneido.language.*;

public class ObjectQ extends SuFunction {

	private static final FunctionSpec fs = new FunctionSpec("value");

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		return args[0] instanceof SuContainer;
	}

}
