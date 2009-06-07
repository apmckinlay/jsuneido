package suneido.language.builtin;

import java.util.Date;

import suneido.language.*;

public class DateQ extends SuFunction {

	private static final FunctionSpec fs = new FunctionSpec("value");

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		return args[0] instanceof Date;
	}

}
