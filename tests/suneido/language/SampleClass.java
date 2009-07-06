package suneido.language;

import suneido.SuContainer;


public class SampleClass extends SuClass {

	@Override
	public Object invoke(Object self, String method, Object... args) {
		if (method == "MyMethod")
			return MyMethod(self, args);
		else if (method == "_init")
			return _init(self, args);
		else
			return super.invoke(self, method, args);
	}

	private Object MyMethod(Object self, Object[] args) {
		args = Args.massage(params[0], args);
		// Object[] consts = constants[0];
		return null;
	}

	private Object _init(Object self, Object[] args) {
		superInvoke(self, "_init", args);
		SuContainer x = new SuContainer();
		for (Object a : args)
			x.append(a);
		((SuInstance) self).put("args", x);
		return null;
	}

}