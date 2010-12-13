package suneido.language.builtin;

import suneido.language.*;

public class Random extends BuiltinFunction {

	private final java.util.Random random = new java.util.Random();

	private static final FunctionSpec fs = new FunctionSpec("range");

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		int n = Ops.toInt(args[0]);
		return n == 0 ? 0 : random.nextInt(n);
	}

}
