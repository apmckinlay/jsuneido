package suneido.language.builtin;

import suneido.SuContainer;
import suneido.TheDbms;
import suneido.language.BuiltinFunction0;

public class Libraries extends BuiltinFunction0 {

	@Override
	public Object call0() {
		return new SuContainer(TheDbms.dbms().libraries());
	}

}
