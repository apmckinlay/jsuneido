package suneido.language;


public class SampleClass extends SuClass {

	@Override
	public Object invoke(Object self, String method, Object... args) {
		if (method == "MyMethod")
			return MyMethod(self, args);
		else if (method == "New")
			return New(self, args);
		else
			return super.invoke(self, method, args);
	}

	private Object MyMethod(Object self, Object[] args) {
		args = Args.massage(params[0], args);
		// Object[] consts = constants[0];
		return null;
	}

	private Object New(Object self, Object[] args) {
		superInvoke(self, "New", args);
		return null;
	}

}