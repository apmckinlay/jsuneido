package suneido.language.builtin;

import suneido.language.SuFunction2;
import suneido.language.Ops;

public class Or extends SuFunction2 {

	@Override
	public Boolean call2(Object a, Object b) {
		return Ops.toBoolean_(a) || Ops.toBoolean_(b);
	}

}
