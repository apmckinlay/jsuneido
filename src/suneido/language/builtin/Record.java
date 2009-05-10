package suneido.language.builtin;

import suneido.SuRecord;
import suneido.language.Args;
import suneido.language.SuFunction;

public class Record extends SuFunction {

	@Override
	public Object call(Object... args) {
		return Args.collectArgs(args, new SuRecord());
	}

}

