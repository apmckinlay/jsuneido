package suneido.language.builtin;

import java.net.InetAddress;

import suneido.database.server.DbmsServer;
import suneido.language.*;

public class ServerIP extends BuiltinFunction {

	@Override
	public Object call(Object... args) {
		Args.massage(FunctionSpec.noParams, args);
		InetAddress inetAddress = DbmsServer.getInetAddress();
		return inetAddress == null ? "" : inetAddress.getHostAddress();
	}

}
