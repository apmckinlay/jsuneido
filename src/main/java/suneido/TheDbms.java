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
import suneido.database.server.DbmsClient;
import suneido.intfc.database.Database;
import suneido.runtime.builtin.SocketServer;
import suneido.util.Util;

public class TheDbms {
	private static final long IDLE_TIMEOUT_MS = 5 * 60 * 1000; // 5 min
	private static String ip = null;
	private static int port = 0;
	private static DbmsLocal localDbms;
	private static final ThreadLocal<DbmsClient> remoteDbms = new ThreadLocal<>();
	private static final Set<DbmsClient> dbmsRemotes =
			Collections.synchronizedSet(new HashSet<DbmsClient>());
	private static final ThreadLocal<byte[]> authToken = new ThreadLocal<>();
	private static final ThreadLocal<String> lastSessionId =
			ThreadLocal.withInitial(() -> "");

	public static Dbms dbms() {
		if (ip == null)
			return localDbms;
		DbmsClient dbms = remoteDbms.get();
		if (dbms == null)
			dbms = newDbms();
		return dbms;
	}

	private static DbmsClient newDbms() {
		DbmsClient dbms;
		dbms = new DbmsClient(ip, port);
		dbmsRemotes.add(dbms);
		remoteDbms.set(dbms);
		dbms.sessionid(dbms.sessionid("") + ":" + Thread.currentThread().getName());
		// auth will only succeed if parent was authorized
		byte[] token = authToken.get();
		if (token != null)
			dbms.auth(Util.bytesToString(token));
		return dbms;
	}

	// used by errlog to avoid opening db just to get sessionid
	public static String sessionid() {
		DbmsClient dbms = remoteDbms.get();
		return dbms == null ? lastSessionId.get() : dbms.sessionid();
	}

	// used when starting a client
	public static void remote(String ip, int port) {
		TheDbms.ip = ip;
		TheDbms.port = port;
	}

	// used when starting a server
	public static void setPort(int port) {
		TheDbms.port = port;
	}

	// used by ServerIP builtin
	public static String serverIP() {
		return ip;
	}

	// used by ServerPort builtin
	public static int serverPort() {
		return port;
	}

	public static void set(Database db) {
		localDbms = new DbmsLocal(db);
	}

	public static boolean isAvailable() {
		return localDbms != null || ip != null;
	}

	public static void setAuthToken(byte[] token) {
		authToken.set(token);
	}

	/** used by {@link SocketServer} */
	public static void closeIfIdle() {
		DbmsClient dr = remoteDbms.get();
		if (dr == null)
			return;
		long t = System.currentTimeMillis();
		if (dr.idleSince == 0)
			dr.idleSince = t;
		else if (t - dr.idleSince > IDLE_TIMEOUT_MS) {
			dbmsRemotes.remove(dr);
			lastSessionId.set(remoteDbms.get().sessionid() + "(closed)");
			remoteDbms.set(null);
			dr.close();
		}
	}

	/**
	 * Run regularly to close connections owned by threads that have ended
	 */
	public static Runnable closer = () -> {
		synchronized(dbmsRemotes) {
			Iterator<DbmsClient> iter = dbmsRemotes.iterator();
			while (iter.hasNext()) {
				DbmsClient dr = iter.next();
				if (! dr.owner.isAlive()) {
					iter.remove();
					dr.close();
				}
			}
		}
	};

}
