package suneido.language.builtin;

import suneido.SuRecord;
import suneido.language.Args;
import suneido.language.SuFunction;

public class Record extends SuFunction {

	@Override
	public String toString() {
		return "Record";
	}

	@Override
	public Object invoke(Object self, String method, Object... args) {
		if (method == "call")
			return call(args);
		else
			return super.invoke(self, method, args);
	}

	@Override
	public Object call(Object... args) {
		return Args.collectArgs(args, new SuRecord());
	}

}

