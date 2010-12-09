package suneido.language.builtin;

import suneido.language.SuFunction1;
import suneido.language.SuClass;

public class ClassQ extends SuFunction1 {

	@Override
	public Object call1(Object a) {
		return a instanceof SuClass;
	}

}
