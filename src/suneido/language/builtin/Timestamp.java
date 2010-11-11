package suneido.language.builtin;

import static suneido.Suneido.theDbms;
import suneido.language.*;

public class Timestamp extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.noParams, args);
		return theDbms.timestamp();
	}

}
