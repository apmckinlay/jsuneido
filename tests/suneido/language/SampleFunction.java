package suneido.language;

import suneido.SuException;

public class SampleFunction extends SuFunction {
	private static FunctionSpec[] params;
	private static Object[][] constants;

	@Override
	public void setup(FunctionSpec[] p, Object[][] c) {
		params = p;
		constants = c;
	}

	@Override
	public String toString() {
		return "SampleFunction";
	}

	@Override
	public Object invoke(String method, Object... args) {
		if (method == "call")
			return invoke(args);
		else
			return super.invoke(method, args);
	}

	private Object invoke(Object... args) {
		try {
			args[0] = args[1];
		} catch (SuException e) {
			args[0] = e.toString();
		}
		return null;
	}

	private Integer test() {
		return 1234567;
	}
}