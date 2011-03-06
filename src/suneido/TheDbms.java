package suneido;

import java.util.*;

import suneido.database.server.*;

public class TheDbms {
	private static String ip = null;
	private static int port;
	private static Dbms theDbms;
	private static ThreadLocal<DbmsRemote> remoteDbms =
		new ThreadLocal<DbmsRemote>() {
			@Override
			protected DbmsRemote initialValue() {
				DbmsRemote dbms = new DbmsRemote(ip, port);
				dbmsRemotes.add(dbms);
				dbms.sessionid(dbms.sessionid("") + ":" +
						Thread.currentThread().getName());
				return dbms;
			};
			@Override
			protected void finalize() throws Throwable {
				get().close();
			};
		};
	private static List<DbmsRemote> dbmsRemotes = new ArrayList<DbmsRemote>();

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

	/**
	 * Run regularly to close connections owned by threads that SocketServer
	 * has timed out.
	 */
	public static Runnable closer = new Runnable() {
		@Override
		public void run() {
			Iterator<DbmsRemote> iter = dbmsRemotes.iterator();
			while (iter.hasNext()) {
				DbmsRemote dbms = iter.next();
				if (! dbms.owner.isAlive())
					dbms.close();
			}
		}
	};

}
