package suneido.language.builtin;

import suneido.TheDbms;
import suneido.language.BuiltinFunction0;

public class Timestamp extends BuiltinFunction0 {

	@Override
	public Object call0() {
		return TheDbms.dbms().timestamp();
	}

}
