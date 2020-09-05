/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.common.base.CharMatcher;

import suneido.*;
import suneido.compiler.Compiler;
import suneido.database.immudb.Database;
import suneido.database.immudb.Record;
import suneido.database.immudb.RecordBuilder;
import suneido.database.immudb.Table;
import suneido.database.immudb.Transaction;
import suneido.database.query.CompileQuery;
import suneido.database.query.Query.Dir;
import suneido.database.query.Request;
import suneido.runtime.Pack;
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
		// temporary work around for Eclipse bug, revert when Eclipse fixed
		Transaction t;
		if (readwrite)
		    t = db.updateTransaction();
		else
		    t = db.readTransaction();
		// Transaction t = readwrite ? db.updateTransaction() : db.readTransaction();
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
	public SuObject connections() {
		return Suneido.server == null ? SuObject.EMPTY
				: new SuObject(Suneido.server.connections()).setReadonly();
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
			DbTools.dumpDatabase(db, "database.su");
		} else
			DbTools.dumpTable(db, filename);
		return "";
	}

	@Override
	public int load(String filename) {
		return DbTools.loadTable(db, filename);
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
		return Suneido.server == null ? 0
				: Suneido.server.killConnections(sessionId);
	}

	@Override
	public List<LibGet> libget(String name) {
		List<LibGet> srcs = new ArrayList<>();
		Record key = new RecordBuilder().add(name).add(-1).build();
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
				if (rec != null) {
					ByteBuffer raw = rec.getRaw(text_fld);
					byte c = raw.get();
					assert c == Pack.Tag.STRING;
					srcs.add(new LibGet(lib, raw));
				}
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
			throw new SuException("Use failed: " + e);
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
	public SuObject info() {
		SuObject info = new SuObject();
		info.put("timeoutMin", Suneido.cmdlineoptions.timeoutMin);
		info.put("maxUpdateTranSec", Suneido.cmdlineoptions.max_update_tran_sec);
		info.put("maxWritesPerTran", Suneido.cmdlineoptions.max_writes_per_tran);
		info.put("currentSize", size());
		return info;
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
	public DbmsTran transaction(int tn) {
		return ServerData.forThread().getTransaction(tn);
	}

	@Override
	public List<Integer> transactions() {
		return db.tranlist();
	}

	@Override
	public void log(String s) {
		Errlog.uncounted(CharMatcher.whitespace().trimTrailingFrom(s));
	}

	@Override
	public Object exec(SuObject c) {
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

	@Override
	public void close() {
		db.close();
	}

}
