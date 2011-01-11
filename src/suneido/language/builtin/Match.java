package suneido.language.builtin;

import suneido.language.SuFunction2;
import suneido.language.Ops;


public class Match extends SuFunction2 {

	@Override
	public Object call2(Object a, Object b) {
		return Ops.match(a, b);
	}

}
