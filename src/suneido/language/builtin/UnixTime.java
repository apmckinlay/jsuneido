package suneido.language.builtin;

import java.util.Date;

import suneido.language.*;

public class UnixTime extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.noParams, args);
		return new Date().getTime() / 1000;
	}

}
