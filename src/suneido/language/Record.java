package suneido.language;

import suneido.SuRecord;
import suneido.SuValue;

public class Record extends SuFunction {

	@Override
	public String toString() {
		return "Record";
	}

	@Override
	public SuValue invoke(String method, SuValue... args) {
		if (method == "call")
			return invoke(args);
		else
			return super.invoke(method, args);
	}

	private SuValue invoke(SuValue... args) {
		return collectArgs(args, new SuRecord());
	}

}

