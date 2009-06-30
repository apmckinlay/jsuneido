package suneido.language.builtin;

import static suneido.util.Util.array;
import suneido.language.*;

public class Cursor extends BuiltinClass {

	@Override
	public CursorInstance newInstance(Object[] args) {
		return new CursorInstance(args);
	}

	private static final FunctionSpec fs =
			new FunctionSpec(array("query", "block"), Boolean.FALSE);

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		CursorInstance f = newInstance(array(args[0]));
		if (args[1] == Boolean.FALSE)
			return f;
		else
			return Ops.call(args[1], f);
	}

}
