package suneido.language.builtin;

import static suneido.database.server.Command.theDbms;
import suneido.SuException;
import suneido.SuValue;
import suneido.database.server.ServerData;
import suneido.language.*;

public class Database extends SuValue {

	private static final FunctionSpec requestFS = new FunctionSpec("request");
	@Override
	public Object call(Object... args) {
		args = Args.massage(requestFS, args);
		String request = Ops.toStr(args[0]);
		theDbms.admin(new ServerData(), request);
		return null;
	}

	@Override
	public Object invoke(Object self, String method, Object... args) {
		if (method == "<new>")
			throw new SuException("cannot create instances of Database");
		return super.invoke(self, method, args);
	}

}
