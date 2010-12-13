package suneido.language.builtin;

import suneido.language.BuiltinFunction2;
import suneido.language.Ops;

public class Lte extends BuiltinFunction2 {

	@Override
	public Object call2(Object a, Object b) {
		return Ops.lte(a, b);
	}

}
