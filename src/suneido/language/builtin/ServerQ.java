package suneido.language.builtin;

import static suneido.CommandLineOptions.Action.SERVER;
import suneido.Suneido;
import suneido.language.SuFunction0;

public class ServerQ extends SuFunction0 {

	@Override
	public Object call0() {
		return Suneido.cmdlineoptions.action == SERVER;
	}

}
