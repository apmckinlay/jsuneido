package suneido.language;

public class SampleClass extends SuClass {
	private static FunctionSpec[] params;
	private static Object[][] constants;

	@Override
	public void setup(FunctionSpec[] p, Object[][] c) {
		params = p;
		constants = c;
	}

	@Override
	public String toString() {
		return "SampleClass";
	}

	@Override
	public Object invoke(String method, Object... args) {
		if (method == "call")
			return call(Args.massage(params[0], args));
		else
			return super.invoke(method, args);
	}

	@Override
	public Object call(Object... args) {
		return null;
	}

}