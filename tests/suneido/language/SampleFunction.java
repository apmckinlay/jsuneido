package suneido.language;

import java.util.List;

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
		args = massage(params[1], args);
		return null;
	}

	private void test(List<Object> v) {
	}
}