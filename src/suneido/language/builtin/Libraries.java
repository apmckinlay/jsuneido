package suneido.language.builtin;

import static suneido.Suneido.theDbms;
import suneido.SuContainer;
import suneido.language.*;

public class Libraries extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.noParams, args);
		return new SuContainer(theDbms.libraries());
	}

}
