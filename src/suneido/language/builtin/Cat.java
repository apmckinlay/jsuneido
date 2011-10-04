package suneido.language.builtin;

import suneido.language.SuFunction2;
import suneido.language.Ops;

public class Cat extends SuFunction2 {

	@Override
	public Object call2(Object a, Object b) {
		return Ops.cat(a, b);
	}

}
