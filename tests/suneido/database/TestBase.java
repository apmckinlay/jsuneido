package suneido.database;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;

public class TestBase {
	DestMem dest;
	protected Database db;

	@Before
	public void open() {
		dest = new DestMem();
		db = new Database(dest, Mode.CREATE);
	}

	@After
	public void close() {
		db.close();
	}

	protected void reopen() {
		db.close();
		db = new Database(dest, Mode.OPEN);
		// try {
		// db = new Database(file.getCanonicalPath(), Mode.OPEN);
		// } catch (IOException e) {
		// throw new SuException("can't reopen", e);
		// }
	}

	protected void makeTable() {
		makeTable(0);
	}

	protected void makeTable(int nrecords) {
		db.addTable("test");
		db.addColumn("test", "a");
		db.addColumn("test", "b");
		db.addIndex("test", "a", true, false);
		db.addIndex("test", "b,a", false, false);

		Transaction t = db.readwriteTran();
		for (int i = 0; i < nrecords; ++i)
			db.addRecord(t, "test", record(i));
		t.ck_complete();
	}

	protected Record record(int i) {
		return new Record().add(i).add("more stuff");
	}

	protected Record key(int i) {
		return new Record().add(i);
	}

	protected List<Record> get() {
		Transaction tran = db.readonlyTran();
		List<Record> recs = get(tran);
		tran.ck_complete();
		return recs;
	}

	protected List<Record> get(Transaction tran) {
		List<Record> recs = new ArrayList<Record>();
		Table tbl = db.getTable("test");
		Index idx = tbl.indexes.first();
		BtreeIndex.Iter iter = idx.btreeIndex.iter(tran).next();
		for (; !iter.eof(); iter.next())
			recs.add(db.input(iter.keyadr()));
		return recs;
	}

	protected void check(int... values) {
		Transaction t = db.readonlyTran();
		check(t, values);
		t.ck_complete();
	}

	protected void check(Transaction t, int... values) {
		List<Record> recs = get(t);
		assertEquals(values.length, recs.size());
		for (int i = 0; i < values.length; ++i)
			assertEquals(record(values[i]), recs.get(i));
	}
}