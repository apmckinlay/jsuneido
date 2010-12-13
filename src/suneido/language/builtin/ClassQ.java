package suneido.language.builtin;

import suneido.language.BuiltinFunction1;
import suneido.language.SuClass;

public class ClassQ extends BuiltinFunction1 {

	@Override
	public Object call1(Object a) {
		return a instanceof SuClass;
	}

}
