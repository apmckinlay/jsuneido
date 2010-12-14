package suneido.language.builtin;

import suneido.SuContainer;
import suneido.TheDbms;
import suneido.language.SuFunction0;

public class Libraries extends SuFunction0 {

	@Override
	public Object call0() {
		return new SuContainer(TheDbms.dbms().libraries());
	}

}
