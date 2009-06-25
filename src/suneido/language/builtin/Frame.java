package suneido.language.builtin;

import suneido.language.*;

public class Frame extends SuFunction {

	private static final FunctionSpec fs = new FunctionSpec("offset");

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		return Boolean.FALSE; // TODO Frame
	}

}
