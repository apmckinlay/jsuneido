package suneido;

import suneido.database.server.*;

public class TheDbms {
	private static String ip = null;
	private static int port;
	private static Dbms theDbms;
	private static ThreadLocal<DbmsRemote> remoteDbms =
		new ThreadLocal<DbmsRemote>() {
			@Override
	                protected DbmsRemote initialValue() {
				return new DbmsRemote(ip, port);
			};
			@Override
                        protected void finalize() throws Throwable {
				get().close();
			};
		};

	public static Dbms dbms() {
		if (ip == null) {
			if (theDbms == null)
				theDbms = new DbmsLocal();
			return theDbms;
		} else
		return remoteDbms.get();
	}

	public static void remote(String ip, int port) {
		TheDbms.ip = ip;
		TheDbms.port = port;
	}

}
