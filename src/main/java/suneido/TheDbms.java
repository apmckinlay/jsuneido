/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import suneido.database.server.Dbms;
import suneido.database.server.DbmsLocal;
import suneido.database.server.DbmsRemote;
import suneido.intfc.database.Database;
import suneido.util.Print;

public class TheDbms {
	private static final long IDLE_TIMEOUT_MS = 5 * 60 * 1000; // 5 min
	private static String ip = null;
	private static int port;
	private static DbmsLocal localDbms;
	private static final ThreadLocal<DbmsRemote> remoteDbms =
			new ThreadLocal<>();
	private static final Set<DbmsRemote> dbmsRemotes =
			Collections.synchronizedSet(new HashSet<DbmsRemote>());

	public static Dbms dbms() {
		if (ip == null)
			return localDbms;
		DbmsRemote dbms = remoteDbms.get();
		if (dbms == null) {
			dbms = new DbmsRemote(ip, port);
			dbmsRemotes.add(dbms);
			dbms.sessionid(dbms.sessionid("") + ":" +
					Thread.currentThread().getName());
			remoteDbms.set(dbms);
		}
		return dbms;
	}

	public static void remote(String ip, int port) {
		TheDbms.ip = ip;
		TheDbms.port = port;
	}

	public static void set(Database db) {
		localDbms = new DbmsLocal(db);
	}

	public static boolean isAvailable() {
		return localDbms != null || ip != null;
	}

	public static void closeIfIdle() {
		DbmsRemote dr = remoteDbms.get();
		if (dr == null)
			return;
		long t = System.currentTimeMillis();
		if (dr.idleSince == 0)
			dr.idleSince = t;
		else if (t - dr.idleSince > IDLE_TIMEOUT_MS) {
			Print.timestamped("closing idle dbms connection for " +
					Thread.currentThread().getName());
			dbmsRemotes.remove(dr);
			remoteDbms.set(null);
			dr.close();
		}
	}

	/**
	 * Run regularly to close connections owned by threads that have ended
	 */
	public static Runnable closer = new Runnable() {
		@Override
		public void run() {
			synchronized(dbmsRemotes) {
				Iterator<DbmsRemote> iter = dbmsRemotes.iterator();
				while (iter.hasNext()) {
					DbmsRemote dr = iter.next();
					if (! dr.owner.isAlive()) {
						iter.remove();
						dr.close();
					}
				}
			}
		}
	};

}
