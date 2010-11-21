package suneido.language.builtin;

import suneido.language.*;

public class Sleep extends BuiltinFunction1 {

	{ functionSpec = new FunctionSpec("ms"); }

	@Override
	public Object call1(Object a) {
		int n = Ops.toInt(a);
		try {
			Thread.sleep(n);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}

}
