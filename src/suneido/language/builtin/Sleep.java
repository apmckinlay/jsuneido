package suneido.language.builtin;

import suneido.language.*;

public class Sleep extends BuiltinFunction {

	private static final FunctionSpec fs = new FunctionSpec("ms");

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		int n = Ops.toInt(args[0]);
		try {
			Thread.sleep(n);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}

}
