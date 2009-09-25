package suneido.language.builtin;

import suneido.WhenBuilt;
import suneido.language.*;

public class Built extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.noParams, args);
		return WhenBuilt.when();
	}

}