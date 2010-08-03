package suneido.language.builtin;

import java.util.UUID;

import suneido.language.*;

public class UuidString extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.noParams, args);
		return UUID.randomUUID().toString();
	}

}