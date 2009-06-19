package suneido.language.builtin;

import static suneido.util.Util.array;
import suneido.language.*;

public class FileClass extends BuiltinClass {

	@Override
	public FileInstance newInstance(Object[] args) {
		return new FileInstance(args);
	}

	private static final FunctionSpec fileFS =
			new FunctionSpec(array("filename", "mode", "block"), "r", false);

	@Override
	public Object call(Object... args) {
		FileInstance f = newInstance(args);
		args = Args.massage(fileFS, args);
		if (args[2] == Boolean.FALSE)
			return f;
		try {
			Object result = Ops.call(args[2], f);
			f.close();
			return result;
		} finally {
			f.close();
		}
	}

}
