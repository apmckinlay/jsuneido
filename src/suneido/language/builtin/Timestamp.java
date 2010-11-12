package suneido.language.builtin;

import suneido.TheDbms;
import suneido.language.*;

public class Timestamp extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.noParams, args);
		return TheDbms.dbms().timestamp();
	}

}
