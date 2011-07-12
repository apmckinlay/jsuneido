package suneido.language.builtin;

import suneido.SuContainer;
import suneido.TheDbms;
import suneido.language.*;

public class DoWithoutTriggers extends SuFunction {

	private static final FunctionSpec fs = new FunctionSpec("tables", "block");

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		SuContainer c = Ops.toContainer(args[0]);
		try {
			for (Object x : c.vec)
				TheDbms.dbms().disableTrigger(Ops.toStr(x));
			return Ops.call(args[1]);
		} finally {
			for (Object x : c.vec)
				TheDbms.dbms().enableTrigger(Ops.toStr(x));
		}
	}

}
