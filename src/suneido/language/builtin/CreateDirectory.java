package suneido.language.builtin;

import java.io.File;

import suneido.language.*;

public class CreateDirectory extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		args = Args.massage(FunctionSpec.string, args);
		String path = Ops.toStr(args[0]);
		return new File(path).mkdir();
	}

}
