package suneido.language.builtin;

import suneido.language.*;

public class Display extends SuFunction {

	private static final FunctionSpec fs = new FunctionSpec("value");

	@Override
	public Object call(Object... args) {
		args = Args.massage(fs, args);
		// doesn't use Ops.display
		// because here we default to double quote to match cSuneido
		if (args[0] instanceof String) {
			String s = (String) args[0];
			if (s.contains("\"") && !s.contains("'"))
				return "'" + s + "'";
			else
				return "\"" + s.replace("\"", "\\\"") + "\"";
		}
		if (args[0] == null)
			return "null";
		return Ops.toStr(args[0]);
	}

}
