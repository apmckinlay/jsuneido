package suneido.language.builtin;

import suneido.language.*;

public class Synchronized extends BuiltinFunction1 {

	{ params = new FunctionSpec("block"); }

	@Override
	public Object call1(Object a) {
		synchronized (Synchronized.class) {
			return Ops.call(a);
		}
	}

}
