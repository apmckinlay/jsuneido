package suneido.database.server;

import static suneido.database.Database.theDB;

import java.util.*;

import suneido.SuException;
import suneido.SuValue;
import suneido.database.Record;
import suneido.database.Transaction;
import suneido.database.query.*;
import suneido.database.query.Query.Dir;
import suneido.language.Library;

/**
 * Connects Suneido to a local database.
 *
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public class DbmsLocal implements Dbms {

	public void admin(ServerData serverData, String s) {
		Request.execute(serverData, s);
	}

	public int request(ServerData serverData, DbmsTran tran, String s) {
		//System.out.println("\t" + s);
		return ((QueryAction) CompileQuery.parse(serverData, s)).execute((Transaction) tran);
	}

	public DbmsTran transaction(boolean readwrite) {
		return readwrite ? theDB.readwriteTran() : theDB.readonlyTran();
	}

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

	public DbmsQuery query(ServerData serverData, DbmsTran tran, String s) {
		//System.out.println("\t" + s);
		return new DbmsQueryLocal(CompileQuery.query((Transaction) tran,
				serverData, s));
	}

	public DbmsQuery cursor(ServerData serverData, String s) {
		//System.out.println("\t" + s);
		return new DbmsQueryLocal(CompileQuery.query(
				new Transaction(theDB.tabledata, theDB.btreeIndexes), 
				serverData, s, true));
	}

	public void erase(DbmsTran tran, long recadr) {
		theDB.removeRecord((Transaction) tran, recadr);
	}

	public long update(DbmsTran tran, long recadr, Record rec) {
		return theDB.updateRecord((Transaction) tran, recadr, rec);
	}

	public SuValue connections() {
		// TODO connnections
		return null;
	}

	public void copy(String filename) {
		// TODO copy file

	}

	public int cursors() {
		return ServerData.forThread().cursorsSize();
	}

	public void dump(String filename) {
		// TODO dump
	}

	public int finalSize() {
		// TODO finalSize
		return 0;
	}

	public int kill(String s) {
		// TODO kill
		return 0;
	}

	public List<LibGet> libget(String name) {
		return Library.libget(name);
	}

	public List<String> libraries() {
		return Library.libraries();
	}

	public SuValue run(String s) {
		// TODO run
		return null;
	}

	public SuValue sessionid(String s) {
		// TODO sessionid
		return null;
	}

	public long size() {
		return theDB.size();
	}

	public Date timestamp() {
		return Timestamp.next();
	}

	public List<Integer> tranlist() {
		// TODO tranlist
		return Collections.emptyList();
	}

	public void log(String s) {
		// TODO log

	}

}
