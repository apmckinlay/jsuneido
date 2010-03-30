package suneido.database.server;

import static suneido.Suneido.errlog;
import static suneido.database.Database.theDB;

import java.util.Date;
import java.util.List;

import suneido.SuContainer;
import suneido.SuException;
import suneido.database.Record;
import suneido.database.Transaction;
import suneido.database.query.*;
import suneido.database.query.Query.Dir;
import suneido.database.tools.DbDump;
import suneido.language.Compiler;
import suneido.language.Library;

/**
 * Connects Suneido to a local database.
 *
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public class DbmsLocal implements Dbms {

	@Override
	public void admin(ServerData serverData, String s) {
		Request.execute(serverData, s);
	}

	@Override
	public int request(ServerData serverData, DbmsTran tran, String s) {
		//System.out.println("\t" + s);
		Query q = CompileQuery.parse((Transaction) tran, serverData, s);
		return ((QueryAction) q).execute();
	}

	@Override
	public DbmsTran transaction(boolean readwrite) {
		return readwrite ? theDB.readwriteTran() : theDB.readonlyTran();
	}

	@Override
	public HeaderAndRow get(ServerData serverData, Dir dir, String query, boolean one, DbmsTran tran) {
		//System.out.println("\t" + query);
		boolean complete = (tran == null);
		if (tran == null)
			tran = theDB.readonlyTran();
		try {
			Query q = CompileQuery.query((Transaction) tran, serverData, query);
			Row row = q.get(dir);
			if (row != null && q.updateable())
				row.recadr = row.getFirstData().off();
			if (one && row != null && q.get(dir) != null)
				throw new SuException("Query1 not unique: " + query);
			Header hdr = q.header();
			return new HeaderAndRow(hdr, row);
		} finally {
			if (complete)
				tran.complete();
		}
	}

	@Override
	public DbmsQuery query(ServerData serverData, DbmsTran tran, String s) {
		//System.out.println("\t" + s);
		return new DbmsQueryLocal(CompileQuery.query((Transaction) tran,
				serverData, s));
	}

	@Override
	public DbmsQuery cursor(ServerData serverData, String s) {
		//System.out.println("\t" + s);
		Transaction t = theDB.readonlyTran();
		try {
			return new DbmsQueryLocal(
					CompileQuery.query(t, serverData, s, true));
		} finally {
			t.complete();
		}
	}

	@Override
	public void erase(DbmsTran tran, long recadr) {
		((Transaction) tran).removeRecord(recadr);
	}

	@Override
	public long update(DbmsTran tran, long recadr, Record rec) {
		return ((Transaction) tran).updateRecord(recadr, rec);
	}

	@Override
	public SuContainer connections() {
		return new SuContainer();
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
		return theDB.finalSize();
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
		return theDB.size();
	}

	@Override
	public Date timestamp() {
		return Timestamp.next();
	}

	@Override
	public List<Integer> tranlist() {
		return theDB.tranlist();
	}

	@Override
	public void log(String s) {
		String sessionId = ServerData.forThread().getSessionId();
		errlog(sessionId + ": " + s);
	}

}
