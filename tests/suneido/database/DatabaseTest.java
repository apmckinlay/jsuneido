package suneido.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import suneido.SuException;

public class DatabaseTest {
	DestMem dest;
	Database db;

	@Before
	public void open() {
		dest = new DestMem();
		db = new Database(dest, Mode.CREATE);
	}

	@After
	public void close() {
		db.close();
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
		makeTable(3);

		Transaction t1 = add_remove();

		// uncommitted NOT visible to other transactions
		Transaction t2 = db.readonlyTran();
		checkBefore(t2);
		Transaction t3 = db.readwriteTran();
		checkBefore(t3);

		t1.ck_complete();

		// committed updates visible to later transactions
		Transaction t4 = db.readonlyTran();
		checkAfter(t4);
		Transaction t5 = db.readwriteTran();
		checkAfter(t5);
		t5.ck_complete();

		// earlier transactions still don't see changes
		// (transactions see data as of their start time)
		checkBefore(t2);
		t2.ck_complete();
		checkBefore(t3);
	}

	@Test
	public void abort() {
		makeTable(3);

		Transaction t1 = add_remove();
		t1.abort(); // should abort outstanding transactions

		// aborted updates not visible
		checkBefore();

		reopen();

		// aborted updates not visible
		checkBefore();
	}

	private Transaction add_remove() {
		Transaction t = db.readwriteTran();
		db.addRecord(t, "test", record(99));
		db.removeRecord(t, "test", "a", new Record().add(1));
		checkAfter(t); // uncommitted ARE visible to their transaction
		return t;
	}

	private void checkBefore() {
		Transaction t = db.readonlyTran();
		checkBefore(t);
		t.ck_complete();
	}

	private void checkBefore(Transaction t) {
		List<Record> recs = get(t);
		assertEquals(3, recs.size());
		for (int i = 0; i < 3; ++i)
			assertEquals(record(i), recs.get(i));
	}
	private void checkAfter(Transaction t) {
		List<Record> recs = get(t);
		assertEquals(3, recs.size());
		assertEquals(record(0), recs.get(0));
		assertEquals(record(2), recs.get(1));
		assertEquals(record(99), recs.get(2));
	}

	// add index to table with existing records
	@Test
	public void add_index() {
		makeTable(3);
		db.addColumn("test", "c");
		db.addIndex("test", "c", false, false, false, "", "", 0);
		Transaction t = db.readonlyTran();
		Table table = db.getTable("test");
		Index index = table.getIndex("c");
		int i = 0;
		BtreeIndex.Iter iter = index.btreeIndex.iter(t).next();
		for (; !iter.eof(); iter.next())
			assertEquals(record(i++), db.input(iter.keyadr()));
	}

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
		assertTrue(t.conflict().contains("read conflict"));
	}

	@Test
	public void delete_conflict() {
		makeTable(5);

		// deleting different records doesn't conflict
		Transaction t1 = db.readwriteTran();
		db.removeRecord(t1, "test", "a", new Record().add(1));
		Transaction t2 = db.readwriteTran();
		db.removeRecord(t2, "test", "a", new Record().add(2));
		t2.ck_complete();
		t1.ck_complete();

		// deleting the same record conflicts
		t1 = db.readwriteTran();
		db.removeRecord(t1, "test", "a", new Record().add(4));
		t2 = db.readwriteTran();
		try {
			db.removeRecord(t2, "test", "a", new Record().add(4));
			assertTrue(false);
		} catch (SuException e) {
			assertTrue(e.toString().contains("delete conflict"));
		}
		t1.ck_complete();
		assertFalse(t2.complete());
		assertTrue(t2.conflict().contains("delete conflict"));
	}

	@Test
	public void add_conflict() {
		makeTable();

		Transaction t1 = db.readwriteTran();
		db.addRecord(t1, "test", record(99));
		Transaction t2 = db.readwriteTran();
		db.addRecord(t2, "test", record(99));
		t1.ck_complete();
		assertFalse(t2.complete());
		assertTrue(t2.conflict().contains("read conflict"));
		// read conflict due to dup check read
	}

	// support methods ==============================================

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

}
