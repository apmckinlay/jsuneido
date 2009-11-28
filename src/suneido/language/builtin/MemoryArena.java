package suneido.language.builtin;

import suneido.language.*;

public class MemoryArena extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.noParams, args);
		return (int) Runtime.getRuntime().totalMemory();
	}

}
