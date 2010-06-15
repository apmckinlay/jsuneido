package suneido.language.builtin;


import suneido.language.*;
import suneido.util.JarPath;

public class ExePath extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.noParams, args);
		return JarPath.jarPath();
	}

}
