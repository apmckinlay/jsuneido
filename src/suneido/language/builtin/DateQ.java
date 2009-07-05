package suneido.language.builtin;

import java.util.Date;

import suneido.language.*;

public class DateQ extends SuFunction {

	@Override
	public Object call(Object... args) {
		args = Args.massage(FunctionSpec.value, args);
		return args[0] instanceof Date;
	}

}
