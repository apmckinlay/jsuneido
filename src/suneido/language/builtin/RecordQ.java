package suneido.language.builtin;

import suneido.SuRecord;
import suneido.language.*;

public class RecordQ extends SuFunction {

	@Override
	public Object call(Object... args) {
		args = Args.massage(FunctionSpec.value, args);
		return args[0] instanceof SuRecord;
	}

}
