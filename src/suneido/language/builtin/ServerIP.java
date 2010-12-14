package suneido.language.builtin;

import java.net.InetAddress;

import suneido.TheDbms;
import suneido.language.SuFunction0;

public class ServerIP extends SuFunction0 {

	@Override
	public Object call0() {
		InetAddress inetAddress = TheDbms.dbms().getInetAddress();
		return inetAddress == null ? "" : inetAddress.getHostAddress();
	}

}
