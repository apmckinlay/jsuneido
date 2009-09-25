package suneido.language.builtin;

import java.util.Date;

import suneido.language.*;

public class DateQ extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		args = Args.massage(FunctionSpec.value, args);
		return args[0] instanceof Date;
	}

}
