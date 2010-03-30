package suneido.language.builtin;

import static suneido.language.Ops.toStr;

import java.io.File;

import suneido.language.*;

public class DeleteFile extends BuiltinFunction {

	private static final FunctionSpec fs = new FunctionSpec("filename");

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		return new File(toStr(args[0])).delete();
	}

}
