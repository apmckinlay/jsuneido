package suneido.language.builtin;

import suneido.language.BuiltinFunction1;
import suneido.language.Ops;

public class Not extends BuiltinFunction1 {

	@Override
	public Boolean call1(Object a) {
		return Ops.not(a);
	}

}
