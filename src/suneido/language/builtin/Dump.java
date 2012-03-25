package suneido.language.builtin;

import static suneido.language.Ops.toStr;
import static suneido.util.Util.array;
import suneido.TheDbms;
import suneido.language.*;

public class Dump extends SuFunction {

	public static final FunctionSpec FS = new FunctionSpec(array("table"), false);

	@Override
	public Object call(Object... args) {
		args = Args.massage(FS, args);
		if (args[0] == Boolean.FALSE)
			TheDbms.dbms().dump("");
		else
			TheDbms.dbms().dump(toStr(args[0]));
		return null;
	}

}
