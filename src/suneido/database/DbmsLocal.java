package suneido.database;

import static suneido.database.Database.theDB;

import java.util.*;

import suneido.SuException;
import suneido.SuValue;
import suneido.database.query.*;
import suneido.database.query.Query.Dir;

public class DbmsLocal implements Dbms {
	private final Map<Integer, Transaction> trans = new HashMap<Integer, Transaction>();

	public void abort(int tran) {
		// TODO Auto-generated method stub

	}

	public boolean admin(String s) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean commit(int tran, String conflict) {
		// TODO Auto-generated method stub
		return false;
	}

	public SuValue connections() {
		// TODO Auto-generated method stub
		return null;
	}

	public void copy(String filename) {
		// TODO Auto-generated method stub

	}

	public DbmsQuery cursor(String s) {
		// TODO Auto-generated method stub
		return null;
	}

	public int cursors() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void dump(String filename) {
		// TODO Auto-generated method stub

	}

	public void erase(int tran, long recadr) {
		// TODO Auto-generated method stub

	}

	public int finalSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	public Row get(Dir dir, String query, boolean one, Header hdr, int tn) {
		Transaction tran = tn <= 0 ? theDB.readonlyTran() : trans.get(tn);
		try {
			DbmsQuery q = ParseQuery.query(query, tran);
			Row row = q.get(dir);
			if (one && row != null && q.get(dir) != null)
				throw new SuException("Query1 not unique: " + query);
			hdr = q.header();
			return row;
		} finally {
			if (tn <= 0)
				tran.complete();
		}
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

	public DbmsQuery query(int tn, String s) {
		return ParseQuery.query(s, trans.get(tn));
	}

	public boolean record_ok(int tran, long recadr) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean refresh(int tran) {
		// TODO Auto-generated method stub
		return false;
	}

	public int request(int tran, String s) {
		// TODO Auto-generated method stub
		return 0;
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

	public int tempdest() {
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

	public int transaction(TranType type, String session_id) {
		// TODO Auto-generated method stub
		return 0;
	}

	public long update(int tran, long recadr, Record rec) {
		// TODO Auto-generated method stub
		return 0;
	}

}
