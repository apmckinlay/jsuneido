package suneido.database.server;

import static suneido.Suneido.errlog;

import java.util.Date;
import java.util.List;

import suneido.SuContainer;
import suneido.database.*;
import suneido.database.query.*;
import suneido.database.tools.DbDump;
import suneido.language.Compiler;
import suneido.language.Library;

/**
 * Connects Suneido to a local database.
 *
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public class DbmsLocal extends Dbms {

	@Override
	public void admin(String s) {
		Request.execute(ServerData.forThread(), s);
	}

	@Override
	public DbmsTran transaction(boolean readwrite) {
		return new DbmsTranLocal(readwrite);
	}

	@Override
	public DbmsQuery cursor(String s) {
		Transaction t = TheDb.db().readonlyTran();
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
			DbDump.dumpDatabase("database.su");
		else
			DbDump.dumpTable(filename);
	}

	@Override
	public int finalSize() {
		return TheDb.db().finalSize();
	}

	@Override
	public int kill(String sessionId) {
		return 0;
	}

	@Override
	public List<LibGet> libget(String name) {
		return Library.libget(name);
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
		return TheDb.db().size();
	}

	@Override
	public Date timestamp() {
		return Timestamp.next();
	}

	@Override
	public List<Integer> tranlist() {
		return TheDb.db().tranlist();
	}

	@Override
	public void log(String s) {
		String sessionId = ServerData.forThread().getSessionId();
		errlog(sessionId + ": " + s);
	}

}
