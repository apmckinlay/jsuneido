package suneido.language.builtin;

import suneido.language.SuFunction2;
import suneido.language.Ops;

public class Mod extends SuFunction2 {

	@Override
	public Object call2(Object a, Object b) {
		return Ops.mod(a, b);
	}

}
