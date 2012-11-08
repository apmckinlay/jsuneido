package suneido.language.builtin;

import suneido.Suneido;
import suneido.WhenBuilt;
import suneido.language.SuFunction0;

public class Built extends SuFunction0 {

	@Override
	public Object call0() {
		return WhenBuilt.when();
	}

}