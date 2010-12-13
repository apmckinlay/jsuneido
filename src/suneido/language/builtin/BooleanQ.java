package suneido.language.builtin;

import suneido.language.BuiltinFunction1;

public class BooleanQ extends BuiltinFunction1 {

	@Override
	public Object call1(Object a) {
		return a instanceof Boolean;
	}

}
