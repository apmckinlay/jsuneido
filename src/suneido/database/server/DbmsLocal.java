package suneido.database.server;

import static suneido.database.Database.theDB;

import java.util.List;

import suneido.SuException;
import suneido.SuValue;
import suneido.database.Record;
import suneido.database.Transaction;
import suneido.database.query.*;
import suneido.database.query.Query.Dir;

/**
 * Connects Suneido to a local database.
 *
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public class DbmsLocal implements Dbms {

	public void admin(String s) {
		Request.execute(s);
	}

	public int request(DbmsTran tran, String s) {
		return ((QueryAction) ParseQuery.parse(s)).execute();
	}

	public DbmsTran transaction(boolean readwrite, String session_id) {
		return readwrite ? theDB.readwriteTran() : theDB.readonlyTran();
	}

	public HeaderAndRow get(Dir dir, String query, boolean one, DbmsTran tran) {
		boolean complete = tran == null;
		if (tran == null)
			tran = theDB.readonlyTran();
		try {
			Query q = ParseQuery.query(query, (Transaction) tran);
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

	public DbmsQuery query(DbmsTran tran, String s) {
		return ParseQuery.query(s, (Transaction) tran);
	}

	public DbmsQuery cursor(String s) {
		return ParseQuery.query(s);
	}

	public void erase(DbmsTran tran, long recadr) {
		theDB.removeRecord((Transaction) tran, recadr);
	}

	public long update(DbmsTran tran, long recadr, Record rec) {
		return theDB.updateRecord((Transaction) tran, recadr, rec);
	}

	public SuValue connections() {
		// TODO Auto-generated method stub
		return null;
	}

	public void copy(String filename) {
		// TODO Auto-generated method stub

	}

	public int cursors() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void dump(String filename) {
		// TODO Auto-generated method stub

	}

	public int finalSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int kill(String s) {
		// TODO Auto-generated method stub
		return 0;
	}

	public List<String> libget(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<String> libraries() {
		// TODO Auto-generated method stub
		return null;
	}

	public void log(String s) {
		// TODO Auto-generated method stub

	}

	public SuValue run(String s) {
		// TODO Auto-generated method stub
		return null;
	}

	public SuValue sessionid(String s) {
		// TODO Auto-generated method stub
		return null;
	}

	public long size() {
		// TODO Auto-generated method stub
		return 0;
	}

	public SuValue timestamp() {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Integer> tranlist() {
		// TODO Auto-generated method stub
		return null;
	}

}
