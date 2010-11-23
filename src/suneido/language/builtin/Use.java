package suneido.language.builtin;

import suneido.language.*;

public class Use extends BuiltinFunction1 {

	{ params = new FunctionSpec("library"); }

	@Override
	public Object call1(Object a) {
		return Library.use(Ops.toStr(a));
	}

}
