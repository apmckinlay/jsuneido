package suneido.language.builtin;

import suneido.WhenBuilt;
import suneido.language.BuiltinFunction0;

public class Built extends BuiltinFunction0 {

	@Override
	public Object call0() {
		return WhenBuilt.when();
	}

}