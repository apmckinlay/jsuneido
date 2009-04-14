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
			return call(args);
		else
			return super.invoke(method, args);
	}

	@Override
	public Object call(Object... args) {
		try {
			args[0] = args[1];
		} catch (BlockReturnException e) {
			return e.returnValue;
		}
		return null;
	}

	private Integer test() {
		return 1234567;
	}
}