package suneido.language.builtin;

import suneido.language.BuiltinFunction1;
import suneido.language.Ops;

public class Display extends BuiltinFunction1 {

	@Override
	public Object call1(Object a) {
		return Ops.display(a);
	}

}
