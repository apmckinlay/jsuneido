package suneido.language;

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
			return invoke(massage(params[0], args));
		else
			return super.invoke(method, args);
	}

	private Object invoke(Object... args) {
		return Ops.toBool(null);
	}

}