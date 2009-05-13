package suneido.language;

import suneido.SuContainer;

public class ObjectFunction extends SuFunction {

	@Override
	public Object call(Object... args) {
		return Args.collectArgs(args, new SuContainer());
	}

}
