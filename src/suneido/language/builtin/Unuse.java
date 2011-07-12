package suneido.language.builtin;

import suneido.TheDbms;
import suneido.language.*;

public class Unuse extends SuFunction1 {

	{ params = new FunctionSpec("library"); }

	@Override
	public Object call1(Object a) {
		if (! TheDbms.dbms().unuse(Ops.toStr(a)))
			return false;
		Globals.clear();
		return true;
	}

}
