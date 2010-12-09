package suneido.language.builtin;

import java.io.File;

import suneido.language.*;

public class DirExistsQ extends SuFunction {

	@Override
	public Object call(Object... args) {
		args = Args.massage(FunctionSpec.string, args);
		String dir = Ops.toStr(args[0]);
		return new File(dir).isDirectory();
	}

}
