package suneido.language.builtin;

import suneido.language.SuFunction1;
import suneido.language.Ops;

public class Neg extends SuFunction1 {

	@Override
	public Object call1(Object a) {
		return Ops.uminus(a);
	}

}
