package suneido.language.builtin;

import suneido.SuContainer;
import suneido.database.Triggers;
import suneido.language.*;

public class DoWithoutTriggers extends BuiltinFunction {

	private static final FunctionSpec fs = new FunctionSpec("tables", "block");

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		SuContainer c = Ops.toContainer(args[0]);
		try {
			for (Object x : c.getVec())
				Triggers.disableTrigger(Ops.toStr(x));
			return Ops.call(args[1]);
		} finally {
			for (Object x : c.getVec())
				Triggers.enableTrigger(Ops.toStr(x));
		}
	}

}
