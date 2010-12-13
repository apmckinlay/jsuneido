package suneido.language.builtin;

import suneido.language.*;

public class Unload extends BuiltinFunction1 {

	{ params = new FunctionSpec("name"); }

	@Override
	public Object call1(Object a) {
		Globals.unload(Ops.toStr(a));
		return null;
	}

}
