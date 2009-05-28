package suneido.language.builtin;

import suneido.SuContainer;
import suneido.language.Args;
import suneido.language.SuFunction;

public class ObjectFunction extends SuFunction {

	@Override
	public Object call(Object... args) {
		return Args.collectArgs(args, new SuContainer());
	}

}
