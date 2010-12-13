package suneido.language.builtin;

import static suneido.language.Ops.toStr;
import static suneido.util.Util.array;
import suneido.database.tools.DbDump;
import suneido.language.*;

public class Dump extends BuiltinFunction {

	public static final FunctionSpec FS = new FunctionSpec(array("table"), false);

	@Override
	public Object call(Object... args) {
		args = Args.massage(FS, args);
		if (args[0] == Boolean.FALSE)
			DbDump.dumpDatabase("database.su");
		else
			DbDump.dumpTable(toStr(args[0]));
		return null;
	}

}
