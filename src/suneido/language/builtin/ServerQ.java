package suneido.language.builtin;

import static suneido.CommandLineOptions.Action.SERVER;
import suneido.Suneido;
import suneido.language.*;

public class ServerQ extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.noParams, args);
		return Suneido.cmdlineoptions.action == SERVER;
	}

}
