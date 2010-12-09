package suneido.language.builtin;

import suneido.Suneido;
import suneido.language.SuFunction0;

public class ServerPort extends SuFunction0 {

	@Override
	public Object call0() {
		return Suneido.cmdlineoptions.serverPort;
	}

}
