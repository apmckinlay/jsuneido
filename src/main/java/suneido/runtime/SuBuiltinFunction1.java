package suneido.runtime;

public abstract class SuBuiltinFunction1 extends SuBuiltinFunction {

	//
	// CONSTRUTORS
	//

	public SuBuiltinFunction1(String name, FunctionSpec params) {
		super(name, params);
	}

	//
	// ANCESTOR CLASS: SuValue
	//

	@Override
	public Object call(Object... args) {
		Args.massage(params, args);
		return call1(args[0]);
	}

	public abstract Object call1(Object a);
}
