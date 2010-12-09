package suneido.language.builtin;

import suneido.language.SuFunction1;
import suneido.language.Ops;

public class Display extends SuFunction1 {

	@Override
	public Object call1(Object a) {
		return Ops.display(a);
	}

}
