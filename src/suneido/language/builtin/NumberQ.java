package suneido.language.builtin;

import suneido.language.SuFunction1;

public class NumberQ extends SuFunction1 {

	@Override
	public Object call1(Object a) {
		return a instanceof Number;
	}

}
