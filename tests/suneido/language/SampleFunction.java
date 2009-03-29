package suneido.language;

import suneido.SuValue;

public class SampleFunction extends SuFunction {
	private static SuValue[][] constants;

	@Override
	public void setConstants(SuValue[][] c) {
		constants = c;
	}

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
		args[1] = args[1].add1();
		return null;
	}

}