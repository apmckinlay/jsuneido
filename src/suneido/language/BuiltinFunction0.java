package suneido.language;

import suneido.language.*;

public abstract class BuiltinFunction0 extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.noParams, args);
		return call0();
	}

	@Override
	public abstract Object call0();

}
