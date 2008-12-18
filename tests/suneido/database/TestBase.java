package suneido.database;

import static org.junit.Assert.assertEquals;
import static suneido.database.Database.theDB;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;

public class TestBase {
	DestMem dest;
	protected Database db;

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

	protected void makeTable(int nrecords) {
		db.addTable("test");
		db.addColumn("test", "a");
		db.addColumn("test", "b");
		db.addIndex("test", "a", true);
		db.addIndex("test", "b,a", false);

		Transaction t = db.readwriteTran();
		for (int i = 0; i < nrecords; ++i)
			db.addRecord(t, "test", record(i));
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

	protected List<Record> get(String tablename) {
		Transaction tran = db.readonlyTran();
		List<Record> recs = get(tablename, tran);
		tran.ck_complete();
		return recs;
	}

	protected List<Record> get(String tablename, Transaction tran) {
		List<Record> recs = new ArrayList<Record>();
		Table tbl = db.getTable(tablename);
		Index idx = tbl.indexes.first();
		BtreeIndex.Iter iter = idx.btreeIndex.iter(tran).next();
		for (; !iter.eof(); iter.next())
			recs.add(db.input(iter.keyadr()));
		return recs;
	}

	protected Record getFirst(String tablename, Transaction tran) {
		Table tbl = db.getTable(tablename);
		Index idx = tbl.indexes.first();
		BtreeIndex.Iter iter = idx.btreeIndex.iter(tran).next();
		return iter.eof() ? null : db.input(iter.keyadr());
	}

	protected void check(int... values) {
		Transaction t = db.readonlyTran();
		check(t, values);
		t.ck_complete();
	}

	protected void check(Transaction t, int... values) {
		List<Record> recs = get("test", t);
		assertEquals(values.length, recs.size());
		for (int i = 0; i < values.length; ++i)
			assertEquals(record(values[i]), recs.get(i));
	}
}