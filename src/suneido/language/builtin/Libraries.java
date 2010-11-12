package suneido.language.builtin;

import suneido.SuContainer;
import suneido.TheDbms;
import suneido.language.*;

public class Libraries extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.noParams, args);
		return new SuContainer(TheDbms.dbms().libraries());
	}

}
