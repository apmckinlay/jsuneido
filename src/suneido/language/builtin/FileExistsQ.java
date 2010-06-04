package suneido.language.builtin;

import java.io.File;

import suneido.language.*;

public class FileExistsQ extends BuiltinFunction {

	private static final FunctionSpec fs = new FunctionSpec("filename");

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		return new File(Ops.toStr(args[0])).exists();
	}

}
