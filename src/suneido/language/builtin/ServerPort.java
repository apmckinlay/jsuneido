package suneido.language.builtin;

import suneido.Suneido;
import suneido.language.BuiltinFunction0;

public class ServerPort extends BuiltinFunction0 {

	@Override
	public Object call0() {
		return Suneido.cmdlineoptions.serverPort;
	}

}
