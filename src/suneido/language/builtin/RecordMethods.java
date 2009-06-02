package suneido.language.builtin;

import suneido.SuRecord;
import suneido.language.Args;
import suneido.language.FunctionSpec;

public class RecordMethods {

	public static Object invoke(SuRecord r, String method, Object... args) {
		if (method == "Update")
			return update(r, args);
		// TODO Records user defined methods
		return ContainerMethods.invoke(r, method, args);
	}

	private static Object update(SuRecord r, Object[] args) {
		Args.massage(FunctionSpec.noParams, args);
		r.update();
		return Boolean.TRUE;
	}

}
