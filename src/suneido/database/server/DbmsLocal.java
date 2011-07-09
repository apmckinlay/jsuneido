/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import static suneido.Suneido.errlog;

import java.net.InetAddress;
import java.util.Date;
import java.util.List;

import suneido.SuContainer;
import suneido.database.Database;
import suneido.database.Transaction;
import suneido.database.query.CompileQuery;
import suneido.database.query.Request;
import suneido.database.tools.DbDump;
import suneido.language.Compiler;
import suneido.language.Library;
import suneido.language.builtin.ServerEval;

/** Connects Suneido to a local database. */
public class DbmsLocal extends Dbms {
	private final Database db;

	public DbmsLocal(Database db) {
		this.db = db;
	}

	private Database db() {
		return db;
	}

	@Override
	public void admin(String s) {
		Request.execute(db(), ServerData.forThread(), s);
	}

	@Override
	public DbmsTran transaction(boolean readwrite) {
		Transaction t = readwrite ? db().readwriteTran() : db().readonlyTran();
		return new DbmsTranLocal(t);
	}

	@Override
	public DbmsQuery cursor(String s) {
		Transaction t = db().readonlyTran();
		try {
			return new DbmsQueryLocal(
					CompileQuery.query(t, ServerData.forThread(), s, true));
		} finally {
			t.complete();
		}
	}

	@Override
	public SuContainer connections() {
		return new SuContainer(DbmsServer.connections());
	}

	@Override
	public void copy(String filename) {
		// TODO copy
	}

	@Override
	public int cursors() {
		return ServerData.forThread().cursorsSize();
	}

	@Override
	public void dump(String filename) {
		if (filename.equals(""))
			DbDump.dumpDatabase(db(), "database.su");
		else
			DbDump.dumpTable(db(), filename);
	}

	@Override
	public int finalSize() {
		return db().finalSize();
	}

	@Override
	public int kill(String sessionId) {
		return 0;
	}

	@Override
	public List<LibGet> libget(String name) {
		return Library.libget(db(), name);
	}

	@Override
	public List<String> libraries() {
		return Library.libraries();
	}

	@Override
	public Object run(String s) {
		return Compiler.eval(s);
	}

	@Override
	public String sessionid(String sessionid) {
		ServerData serverData = ServerData.forThread();
		if (!sessionid.equals(""))
			serverData.setSessionId(sessionid);
		return serverData.getSessionId();
	}

	@Override
	public long size() {
		return db().size();
	}

	@Override
	public Date timestamp() {
		return Timestamp.next();
	}

	@Override
	public List<Integer> tranlist() {
		return db().tranlist();
	}

	@Override
	public void log(String s) {
		String sessionId = ServerData.forThread().getSessionId();
		errlog(sessionId + ": " + s);
	}

	@Override
	public InetAddress getInetAddress() {
		return null;
	}

	@Override
	public Object exec(SuContainer c) {
		return ServerEval.exec(c);
	}

}
