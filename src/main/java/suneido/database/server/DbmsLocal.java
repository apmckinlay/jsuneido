/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import static suneido.Suneido.dbpkg;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import suneido.*;
import suneido.compiler.Compiler;
import suneido.database.query.CompileQuery;
import suneido.database.query.Query.Dir;
import suneido.database.query.Request;
import suneido.intfc.database.Database;
import suneido.intfc.database.Record;
import suneido.intfc.database.Table;
import suneido.intfc.database.Transaction;
import suneido.runtime.builtin.ServerEval;
import suneido.util.Errlog;

/** Connects Suneido to a local database. */
public class DbmsLocal extends Dbms {
	private final Database db;
	private static final List<String> libraries =
			new CopyOnWriteArrayList<String>(new String[] { "stdlib" });

	public DbmsLocal(Database db) {
		this.db = db;
	}

	@Override
	public void admin(String s) {
		Request.execute(db, ServerData.forThread(), s);
	}

	@Override
	public DbmsTran transaction(boolean readwrite) {
		Transaction t = readwrite ? db.updateTransaction() : db.readTransaction();
		return new DbmsTranLocal(t);
	}

	@Override
	public DbmsQuery cursor(String s) {
		Transaction t = db.readTransaction();
		try {
			return new DbmsQueryLocal(
					CompileQuery.query(t, ServerData.forThread(), s, true));
		} finally {
			t.complete();
		}
	}

	@Override
	public SuContainer connections() {
		return new SuContainer(Suneido.server.connections());
	}

	@Override
	public void copy(String filename) {
		throw new UnsupportedOperationException("copy");
	}

	@Override
	public int cursors() {
		return ServerData.forThread().cursorsSize();
	}

	@Override
	public String dump(String filename) {
		if (filename.equals("")) {
			String check = db.check();
			if (! check.equals(""))
				return check;
			DbTools.dumpDatabase(dbpkg, db, "database.su");
		} else
			DbTools.dumpTable(dbpkg, db, filename);
		return "";
	}

	@Override
	public int load(String filename) {
		return DbTools.loadTable(dbpkg, db, filename);
	}
	
	@Override
	public String check() {
		return db.check();
	}

	@Override
	public int finalSize() {
		return db.finalSize();
	}

	@Override
	public int kill(String sessionId) {
		return Suneido.server.killConnections(sessionId);
	}

	@Override
	public List<LibGet> libget(String name) {
		List<LibGet> srcs = new ArrayList<>();
		Record key = dbpkg.recordBuilder().add(name).add(-1).build();
		Transaction tran = db.readTransaction();
		try {
			for (String lib : libraries) {
				Table table = tran.getTable(lib);
				if (table == null)
					continue;
				List<String> flds = table.getFields();
				int group_fld = flds.indexOf("group");
				int text_fld = flds.indexOf("text");
				if (group_fld < 0 || text_fld < 0)
					continue; // library is invalid, ignore it
				Record rec = tran.lookup(table.num(), "name,group", key);
				if (rec != null)
					srcs.add(new LibGet(lib, rec.getRaw(text_fld)));
			}
		} finally {
			tran.complete();
		}
		return srcs;
	}

	@Override
	public List<String> libraries() {
		return libraries;
	}

	@Override
	public boolean use(String library) {
		if (libraries.contains(library))
			return false;
		try {
			get(Dir.NEXT, library + " project group, name, text", false);
			admin("ensure " + library + " key(name,group)");
		} catch (RuntimeException e) {
			return false;
		}
		libraries.add(library);
		return true;
	}

	@Override
	public boolean unuse(String library) {
		if ("stdlib".equals(library) || ! libraries.contains(library))
			return false;
		libraries.remove(library);
		return true;
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
		return db.size();
	}

	@Override
	public SuDate timestamp() {
		return Timestamp.next();
	}

	@Override
	public List<Integer> tranlist() {
		return db.tranlist();
	}

	@Override
	public void log(String s) {
		Errlog.bare(s);
	}

	@Override
	public InetAddress getInetAddress() {
		return null;
	}

	@Override
	public Object exec(SuContainer c) {
		return ServerEval.exec(c);
	}

	@Override
	public void disableTrigger(String table) {
		db.disableTrigger(table);
	}

	@Override
	public void enableTrigger(String table) {
		db.enableTrigger(table);
	}

	@Override
	public byte[] nonce() {
		throw new SuException("nonce only allowed on clients");
	}

	@Override
	public boolean auth(String data) {
		throw new SuException("auth only allowed on clients");
	}

	@Override
	public byte[] token() {
		return Auth.token();
	}

	Database getDb() {
		return db;
	}

}
