package suneido.language.builtin;

import suneido.TheDbms;
import suneido.language.SuFunction0;

public class Timestamp extends SuFunction0 {

	@Override
	public Object call0() {
		return TheDbms.dbms().timestamp();
	}

}
