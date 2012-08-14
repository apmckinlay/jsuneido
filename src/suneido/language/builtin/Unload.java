package suneido.language.builtin;

import suneido.Suneido;
import suneido.language.FunctionSpec;
import suneido.language.Ops;
import suneido.language.SuFunction1;

public class Unload extends SuFunction1 {

	{ params = new FunctionSpec("name"); }

	@Override
	public Object call1(Object a) {
		Suneido.context.clear(Ops.toStr(a));
		return null;
	}

}
