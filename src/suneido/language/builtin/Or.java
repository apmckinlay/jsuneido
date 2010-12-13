package suneido.language.builtin;

import suneido.language.BuiltinFunction2;
import suneido.language.Ops;

public class Or extends BuiltinFunction2 {

	@Override
	public Boolean call2(Object a, Object b) {
		return Ops.toBoolean_(a) || Ops.toBoolean_(b);
	}

}
