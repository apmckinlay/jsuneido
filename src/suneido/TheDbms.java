package suneido;

import suneido.database.server.*;

public class TheDbms {
	private static Dbms theDbms;

	public static Dbms dbms() {
		if (theDbms == null)
			theDbms = new DbmsLocal();
		return theDbms;
	}

	public static void remote(String ip, int port) {
		theDbms = new DbmsRemote(ip, port);
	}

	public static void close() {
		if (theDbms instanceof DbmsRemote)
			((DbmsRemote) theDbms).close();
	}

}
