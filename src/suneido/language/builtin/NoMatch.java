package suneido.language.builtin;

import suneido.language.BuiltinFunction2;
import suneido.language.Ops;

public class NoMatch extends BuiltinFunction2 {

	@Override
	public Object call2(Object a, Object b) {
		return Ops.matchnot(a, b);
	}

}
