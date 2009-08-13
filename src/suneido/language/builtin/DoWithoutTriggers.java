package suneido.language.builtin;

import suneido.SuContainer;
import suneido.database.Table;
import suneido.language.*;

public class DoWithoutTriggers extends SuFunction {

	private static final FunctionSpec fs = new FunctionSpec("tables", "block");

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		SuContainer c = Ops.toContainer(args[0]);
		try {
			for (Object x : c.getVec())
				Table.disableTrigger(Ops.toStr(x));
			return Ops.call(args[1]);
		} finally {
			for (Object x : c.getVec())
				Table.enableTrigger(Ops.toStr(x));
		}
	}

}
