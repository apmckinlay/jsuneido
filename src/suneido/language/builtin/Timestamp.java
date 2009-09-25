package suneido.language.builtin;

import static suneido.database.server.Command.theDbms;
import suneido.language.*;

public class Timestamp extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.noParams, args);
		return theDbms.timestamp();
	}

}
