package suneido.language.builtin;

import suneido.SuRecord;
import suneido.language.Args;
import suneido.language.BuiltinClass;

public class Record extends BuiltinClass {

	@Override
	public Object call(Object... args) {
		return newInstance(args);
	}

	@Override
	public Object newInstance(Object[] args) {
		return Args.collectArgs(new SuRecord(), args);
	}

}

