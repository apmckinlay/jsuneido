package suneido.language.builtin;

import static suneido.util.Util.array;
import suneido.Suneido;
import suneido.language.FunctionSpec;
import suneido.language.Ops;
import suneido.language.SuFunction1;

public class Unload extends SuFunction1 {

	{ params = new FunctionSpec(array("name"), false); }

	@Override
	public Object call1(Object a) {
		if (a == Boolean.FALSE)
			Suneido.context.clearAll();
		else
			Suneido.context.clear(Ops.toStr(a));
		return null;
	}

}
