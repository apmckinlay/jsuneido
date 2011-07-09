package suneido.language.builtin;

import suneido.language.*;

public class Unuse extends SuFunction1 {

	{ params = new FunctionSpec("library"); }

	@Override
	public Object call1(Object a) {
		return suneido.language.Library.unuse(Ops.toStr(a));
	}

}
