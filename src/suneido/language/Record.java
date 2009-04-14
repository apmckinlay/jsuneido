package suneido.language;

import suneido.SuRecord;

public class Record extends SuFunction {

	@Override
	public String toString() {
		return "Record";
	}

	@Override
	public Object invoke(String method, Object... args) {
		if (method == "call")
			return invoke(args);
		else
			return super.invoke(method, args);
	}

	private Object invoke(Object... args) {
		return Args.collectArgs(args, new SuRecord());
	}

}

