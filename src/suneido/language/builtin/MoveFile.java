package suneido.language.builtin;

import java.io.File;

import suneido.language.*;

public class MoveFile extends SuFunction {

	private static final FunctionSpec fs = new FunctionSpec("from", "to");

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		String from = Ops.toStr(args[0]);
		String to = Ops.toStr(args[1]);
		return new File(from).renameTo(new File(to));
	}

}
