package suneido.language;

public abstract class BuiltinFunction2 extends BuiltinFunction {

	protected FunctionSpec functionSpec = FunctionSpec.value2;

	@Override
	public Object call(Object... args) {
		args = Args.massage(functionSpec, args);
		return call2(args[0], args[1]);
	}

	@Override
	public abstract Object call2(Object a, Object b);

}
