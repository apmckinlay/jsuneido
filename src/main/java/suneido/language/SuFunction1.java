package suneido.language;

public abstract class SuFunction1 extends SuFunction {

	{ params = FunctionSpec.value; }

	@Override
	public Object call(Object... args) {
		args = Args.massage(params, args);
		return call1(args[0]);
	}

	@Override
	public Object call0() {
		return call1(fillin(0));
	}

	@Override
	public abstract Object call1(Object a);

}
