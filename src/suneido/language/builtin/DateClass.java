package suneido.language.builtin;

import static suneido.util.Util.array;

import java.util.Date;

import suneido.language.*;

public class DateClass extends BuiltinClass {

	private static final FunctionSpec stringFS =
			new FunctionSpec(array("string"), false);

	@Override
	public Object newInstance(Object[] args) {
		args = Args.massage(stringFS, args);
		// TODO string argument
		// TODO named arguments
		return new Date();
	}

}
