package suneido.language;

import suneido.SuValue;

public class SampleClass extends SuClass {
	private static FunctionSpec[] params;
	private static SuValue[][] constants;

	@Override
	public SuClass newInstance() {
		return new SampleClass();
	}

	@Override
	public void setup(FunctionSpec[] p, SuValue[][] c) {
		params = p;
		constants = c;
	}

	@Override
	public String toString() {
		return "SampleClass";
	}

	@Override
	public SuValue invoke(String method, SuValue... args) {
		if (method == "call")
			return invoke(massage(params[0], args));
		else
			return super.invoke(method, args);
	}

	private SuValue invoke(SuValue... args) {
		return null;
	}

}