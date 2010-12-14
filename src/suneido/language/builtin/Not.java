package suneido.language.builtin;

import suneido.language.SuFunction1;
import suneido.language.Ops;

public class Not extends SuFunction1 {

	@Override
	public Boolean call1(Object a) {
		return Ops.not(a);
	}

}
