package suneido.language;

public abstract class SuFunction2 extends SuFunction {

	{ params = FunctionSpec.value2; }

	@Override
	public Object call(Object... args) {
		args = Args.massage(params, args);
		return call2(args[0], args[1]);
	}

	@Override
	public Object call0() {
		return call2(defaultFor(0), defaultFor(1));
	}

	@Override
	public Object call1(Object a) {
		return call1(defaultFor(0));
	}

	@Override
	public abstract Object call2(Object a, Object b);

}
