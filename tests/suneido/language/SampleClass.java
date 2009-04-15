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
	public Object invoke(Object self, String method, Object... args) {
		if (method == "call")
			return call(Args.massage(params[0], args));
		else if (method == "MyMethod")
			return MyMethod(self, args);
		else
			return super.invoke(self, method, args);
	}

	@Override
	public Object call(Object... args) {
		return null;
	}

	private static Object MyMethod(Object self, Object... args) {
		return null;
	}

}