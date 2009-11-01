package suneido.database;

import static org.junit.Assert.assertEquals;
import static suneido.database.Database.theDB;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;

import suneido.database.query.*;
import suneido.database.server.ServerData;

public class TestBase {
	DestMem dest;
	protected Database db;
	protected final ServerData serverData = new ServerData();

	@Before
	public void create() {
		dest = new DestMem();
		theDB = db = new Database(dest, Mode.CREATE);
	}

	@After
	public void close() {
		db.close();
	}

	protected void reopen() {
		db.close();
		db = new Database(dest, Mode.OPEN);
	}

	protected void makeTable() {
		makeTable(0);
	}

	protected void makeTable(String tablename) {
		makeTable(tablename, 0);
	}

	protected void makeTable(int nrecords) {
		makeTable("test", nrecords);
	}
	protected void makeTable(String tablename, int nrecords) {
		db.addTable(tablename);
		db.addColumn(tablename, "a");
		db.addColumn(tablename, "b");
		db.addIndex(tablename, "a", true);
		db.addIndex(tablename, "b,a", false);

		Transaction t = db.readwriteTran();
		for (int i = 0; i < nrecords; ++i)
			t.addRecord(tablename, record(i));
		t.ck_complete();
	}

	protected Record record(int i) {
		return new Record().add(i).add("more stuff");
	}

	protected Record record(int... values) {
		Record r = new Record();
		for (int i : values)
			r.add(i);
		return r;
	}

	protected Record key(int i) {
		return new Record().add(i);
	}

	protected List<Record> get() {
		return get("test");
	}

	protected List<Record> get(String tablename) {
		Transaction tran = db.readonlyTran();
		List<Record> recs = get(tablename, tran);
		tran.ck_complete();
		return recs;
	}

	protected List<Record> get(Transaction tran) {
		return get("test", tran);
	}

	protected List<Record> get(String tablename, Transaction tran) {
		List<Record> recs = new ArrayList<Record>();
		Table table = tran.getTable(tablename);
		Index index = table.indexes.first();
		BtreeIndex bti = tran.getBtreeIndex(index);
		BtreeIndex.Iter iter = bti.iter(tran).next();
		for (; !iter.eof(); iter.next())
			recs.add(db.input(iter.keyadr()));
		return recs;
	}

	protected Record getFirst(String tablename, Transaction tran) {
		Table table = tran.getTable(tablename);
		Index index = table.indexes.first();
		BtreeIndex bti = tran.getBtreeIndex(index);
		BtreeIndex.Iter iter = bti.iter(tran).next();
		return iter.eof() ? null : db.input(iter.keyadr());
	}

	protected Record getLast(String tablename, Transaction tran) {
		Table table = tran.getTable(tablename);
		Index index = table.indexes.first();
		BtreeIndex bti = tran.getBtreeIndex(index);
		BtreeIndex.Iter iter = bti.iter(tran).prev();
		return iter.eof() ? null : db.input(iter.keyadr());
	}

	protected void check(int... values) {
		check("test", values);
	}
	protected void check(String filename, int... values) {
		Transaction t = db.readonlyTran();
		check(t, filename, values);
		t.ck_complete();
	}

	protected void check(Transaction t, String filename, int... values) {
		List<Record> recs = get(filename, t);
		assertEquals("number of values", values.length, recs.size());
		for (int i = 0; i < values.length; ++i)
			assertEquals(record(values[i]), recs.get(i));
	}

	protected int req(String s) {
		Transaction tran = db.readwriteTran();
		try {
			Query q = CompileQuery.parse(tran, serverData, s);
			int n = ((QueryAction) q).execute();
			tran.ck_complete();
			return n;
		} finally {
			tran.abortIfNotComplete();
		}
	}

}