package suneido.language;

public abstract class BuiltinFunction1 extends BuiltinFunction {

	{ params = FunctionSpec.value; }

	@Override
	public Object call(Object... args) {
		args = Args.massage(params, args);
		return call1(args[0]);
	}

	@Override
	public abstract Object call1(Object a);

}
