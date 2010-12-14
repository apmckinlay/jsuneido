package suneido.language.builtin;

import suneido.language.SuFunction1;
import suneido.util.Util;

public class Pack extends SuFunction1 {

	@Override
	public Object call1(Object a) {
		return Util.bytesToString(suneido.language.Pack.pack(a));
	}

}
