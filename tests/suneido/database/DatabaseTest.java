package suneido.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DatabaseTest {
	// File file = null;
	DestMem dest;
	Database db;

	@Before
	public void open() {
		// file = File.createTempFile("mmf", null);
		// db = new Database(file.getCanonicalPath(), Mode.CREATE);
		dest = new DestMem();
		db = new Database(dest, Mode.CREATE);
	}

	@After
	public void close() {
		// db.close();
		// file.delete();
	}

	private void reopen() {
		db.close();
		db = new Database(dest, Mode.OPEN);
		// try {
		// db = new Database(file.getCanonicalPath(), Mode.OPEN);
		// } catch (IOException e) {
		// throw new SuException("can't reopen", e);
		// }
	}

	@Test
	public void output_input() {
		Record r = new Record().add("hello");
		long offset = db.output(1234, r);

		reopen();

		ByteBuffer bb = db.adr(offset - 4);
		assertEquals(1234, bb.getInt());

		Record r2 = db.input(offset);
		assertEquals(r, r2);
	}

	@Test
	public void test() {
		Table tbl = db.getTable("indexes");
		assertEquals("indexes", tbl.name);
		assertSame(tbl, db.getTable(2));

		makeTable();

		tbl = db.getTable("test");
		assertEquals(2, tbl.columns.size());
		assertEquals(2, tbl.indexes.size());

		Record r = new Record().add(12).add(34);
		Transaction t = db.readwriteTran();
		db.addRecord(t, "test", r);
		t.ck_complete();

		reopen();

		tbl = db.getTable("test");
		assertEquals(1, tbl.nrecords);

		List<Record> recs = get();
		assertEquals(1, recs.size());
		assertEquals(r, recs.get(0));

		t = db.readwriteTran();
		db.removeRecord(t, "test", "a", new Record().add(12));
		t.ck_complete();

		assertEquals(0, get().size());
	}

	@Test
	public void visibility() {
		makeTable();

		Record r = new Record().add(12).add(34);

		Transaction t1 = db.readwriteTran();
		db.addRecord(t1, "test", r);

		// uncommitted ARE visible to their transaction
		assertEquals(1, get(t1).size());

		// uncommitted NOT visible to other transactions
		Transaction t2 = db.readonlyTran();
		assertEquals(0, get(t2).size());
		Transaction t3 = db.readwriteTran();
		assertEquals(0, get(t3).size());

		t1.ck_complete();

		// committed updates visible to later transactions
		Transaction t4 = db.readonlyTran();
		assertEquals(1, get(t4).size());
		t4.ck_complete();
		Transaction t5 = db.readwriteTran();
		assertEquals(1, get(t5).size());
		t5.ck_complete();

		// transactions see data as of their start time
		assertEquals(0, get(t2).size());
		t2.ck_complete();
		assertEquals(0, get(t3).size());
		t3.ck_complete();
	}

	@Test
	public void abort() { // TODO aborted delete
		makeTable();

		Transaction t1 = db.readwriteTran();
		db.addRecord(t1, "test", record(0));
		t1.abort();

		// aborted updates not visible
		Transaction t2 = db.readonlyTran();
		assertEquals(0, get(t2).size());
		t2.ck_complete();

		reopen();

		// aborted updates not visible
		Transaction t3 = db.readonlyTran();
		assertEquals(0, get(t3).size());
		t3.ck_complete();
	}

	// TODO add index to table with existing records

	@Test
	public void validate_reads() {
		makeTable(3);

		Transaction t = db.readwriteTran();
		get(t);

		Transaction t2 = db.readwriteTran();
		db.removeRecord(t2, "test", "a", new Record().add(1));
		t2.ck_complete();

		// need to do a write or else it won't validate_reads
		db.addRecord(t, "test", record(99));
		assertFalse(t.complete());
	}

	private void makeTable() {
		makeTable(0);
	}

	private void makeTable(int nrecords) {
		db.addTable("test");
		db.addColumn("test", "a");
		db.addColumn("test", "b");
		db.addIndex("test", "a", true, false, false, "", "", 0);
		db.addIndex("test", "b,a", false, false, false, "", "", 0);

		Transaction t = db.readwriteTran();
		for (int i = 0; i < nrecords; ++i)
			db.addRecord(t, "test", record(i));
		t.ck_complete();
	}

	private Record record(int i) {
		return new Record().add(i).add("more stuff");
	}

	private List<Record> get() {
		Transaction tran = db.readonlyTran();
		List<Record> recs = get(tran);
		tran.ck_complete();
		return recs;
	}

	private List<Record> get(Transaction tran) {
		List<Record> recs = new ArrayList<Record>();
		Table tbl = db.getTable("test");
		Index idx = tbl.indexes.first();
		BtreeIndex.Iter iter = idx.btreeIndex.iter(tran).next();
		for (; !iter.eof(); iter.next())
			recs.add(db.input(iter.keyadr()));
		return recs;
	}

	// public static void main(String args[]) {
	// new DatabaseTest().output_input();
	// }
}
