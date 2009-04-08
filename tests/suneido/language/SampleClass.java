package suneido.language;

public class SampleClass extends SuClass {
	private static FunctionSpec[] params;
	private static Object[][] constants;

	@Override
	public SuClass newInstance() {
		return new SampleClass();
	}

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
			return invoke(massage(params[0], args));
		else
			return super.invoke(method, args);
	}

	private Object invoke(Object... args) {
		return null;
	}

}