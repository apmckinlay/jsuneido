package suneido.language.builtin;

import suneido.language.*;

public class ClassQ extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		args = Args.massage(FunctionSpec.value, args);
		return args[0] instanceof SuClass;
	}

}
