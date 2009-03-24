package suneido.language;

import suneido.SuValue;

public class SampleFunction2 extends SuFunction {
	@Override
	public String toString() {
		return "SampleFunction";
	}

	@Override
	public SuValue invoke(String method, SuValue... args) {
		if (method == "call")
			return invoke(args);
		else
			return super.invoke(method, args);
	}

	@Override
	public SuValue invoke(SuValue... args) {
		return null;
	}

}