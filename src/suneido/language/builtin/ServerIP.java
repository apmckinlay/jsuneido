package suneido.language.builtin;

import java.net.InetAddress;

import suneido.TheDbms;
import suneido.language.BuiltinFunction0;

public class ServerIP extends BuiltinFunction0 {

	@Override
	public Object call0() {
		InetAddress inetAddress = TheDbms.dbms().getInetAddress();
		return inetAddress == null ? "" : inetAddress.getHostAddress();
	}

}
