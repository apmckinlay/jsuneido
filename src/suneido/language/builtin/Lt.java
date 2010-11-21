package suneido.language.builtin;

import suneido.language.BuiltinFunction2;
import suneido.language.Ops;

public class Lt extends BuiltinFunction2 {

	@Override
	public Object call2(Object a, Object b) {
		return Ops.lt(a, b);
	}

}
