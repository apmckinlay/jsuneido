package suneido.runtime.builtin;

import suneido.runtime.Ops;
import suneido.runtime.Params;
import suneido.util.Errlog;

public class ErrorLog {

	@Params("string")
	public static Object ErrorLog(Object a) {
		Errlog.error(Ops.toStr(a));
		return null;
	}
}
