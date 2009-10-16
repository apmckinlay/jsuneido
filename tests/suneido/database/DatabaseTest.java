package suneido.database;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.util.List;

import org.junit.Test;

import suneido.SuException;
import suneido.database.Index.ForeignKey;

public class DatabaseTest extends TestBase {

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
		assertSame(tbl, db.getTable(3));

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
		assertEquals(1, db.tabledataMaster.get(tbl.num).nrecords);

		List<Record> recs = get("test");
		assertEquals(1, recs.size());
		assertEquals(r, recs.get(0));

		t = db.readwriteTran();
		db.removeRecord(t, "test", "a", new Record().add(12));
		t.ck_complete();

		assertEquals(0, get("test").size());
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
		db.removeRecord(t, "test", "a", key(1));
		checkAfter(t); // uncommitted ARE visible to their transaction
		return t;
	}

	private void checkBefore() {
		check(0, 1, 2);
	}

	private void checkBefore(Transaction t) {
		check(t, 0, 1, 2);
	}

	private void checkAfter(Transaction t) {
		check(t, 0, 2, 99);
	}

	// add index to table with existing records
	@Test
	public void add_index() {
		makeTable(3);
		db.addColumn("test", "c");
		db.addIndex("test", "c", false);
		Transaction t = db.readonlyTran();
		Table table = db.getTable("test");
		Index index = table.getIndex("c");
		int i = 0;
		BtreeIndex.Iter iter = index.btreeIndex.iter(t).next();
		for (; !iter.eof(); iter.next())
			assertEquals(record(i++), db.input(iter.keyadr()));
	}

	@Test
	public void duplicate_key() {
		makeTable(3);

		Transaction t = db.readwriteTran();
		try {
			db.addRecord(t, "test", record(1));
		} catch (SuException e) {
			assertTrue(e.toString().contains("duplicate key: a"));
		}
		t.ck_complete();
	}

	@Test
	public void validate_reads() {
		makeTable(3);

		Transaction t = db.readwriteTran();
		get("test", t);

		Transaction t2 = db.readwriteTran();
		db.removeRecord(t2, "test", "a", key(1));
		t2.ck_complete();

		// need to do a write or else it won't validate_reads
		db.addRecord(t, "test", record(99));
		assertNotNull(t.complete());
		assertTrue(t.conflict().contains("read conflict"));
	}

	@Test
	public void delete_conflict() {
		makeTable(5);

		// deleting different records doesn't conflict
		Transaction t1 = db.readwriteTran();
		db.removeRecord(t1, "test", "a", key(1));
		Transaction t2 = db.readwriteTran();
		db.removeRecord(t2, "test", "a", key(2));
		t2.ck_complete();
		t1.ck_complete();

		// deleting the same record conflicts
		t1 = db.readwriteTran();
		db.removeRecord(t1, "test", "a", key(4));
		t2 = db.readwriteTran();
		try {
			db.removeRecord(t2, "test", "a", key(4));
			assertTrue(false);
		} catch (SuException e) {
			assertTrue(e.toString().contains("delete conflict"));
		}
		t1.ck_complete();
		assertNotNull(t2.complete());
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
		assertNotNull(t2.complete());
		assertTrue(t2.conflict().contains("read conflict"));
		// read conflict due to dup check read
	}

	@Test
	public void read_conflict() {
		makeTable(1);

		Transaction t1 = db.readwriteTran();
		assertNotNull(getFirst("test", t1));

		Transaction t2 = db.readwriteTran();
		db.addRecord(t2, "test", record(-1));
		t2.ck_complete();

		db.addRecord(t1, "test", record(9));
		assertNotNull(t1.complete());
		assertTrue(t1.conflict().contains("read conflict"));
		// read conflict due to dup check read
	}

	@Test
	public void update_conflict() {
		makeTable(3);

		Transaction t1 = db.readwriteTran();
		db.updateRecord(t1, "test", "a", key(1), record(5));

		Transaction t2 = db.readwriteTran();
		try {
			db.updateRecord(t2, "test", "a", key(1), record(6));
		} catch (SuException e) {
			// ignore, should be
		}
		assertNotNull(t2.complete());
		assertTrue(t2.conflict().contains("delete conflict"));

		t1.ck_complete();
	}

	@Test
	public void update() {
		makeTable(3);
		Transaction t = db.readwriteTran();
		db.updateRecord(t, "test", "a", key(1), record(5));
		checkBefore();
		t.ck_complete();
		check(0, 2, 5);
	}

	@Test
	public void foreign_key_addRecord() {
		makeTable(3);

		db.addTable("test2");
		db.addColumn("test2", "a");
		db.addColumn("test2", "f1");
		db.addColumn("test2", "f2");
		db.addIndex("test2", "a", true);
		db.addIndex("test2", "f1", false, false, false, "test", "a",
				Index.BLOCK);
		db.addIndex("test2", "f2", false, false, false, "test", "a",
				Index.BLOCK);

		Table table = db.getTable("test2");
		ForeignKey fk = table.indexes.get("f1").fksrc;
		assertEquals("test", fk.tablename);
		assertEquals("a", fk.columns);
		assertEquals(0, fk.mode);
		fk = table.indexes.get("f2").fksrc;
		assertEquals("test", fk.tablename);
		assertEquals("a", fk.columns);
		assertEquals(0, fk.mode);

		Transaction t1 = db.readwriteTran();
		db.addRecord(t1, "test2", record(10, 1, 2));
		shouldBlock(t1, record(11, 5, 1));
		shouldBlock(t1, record(11, 1, 5));

		try {
			db.removeRecord(t1, "test", "a", key(1));
			assertTrue(false);
		} catch (SuException e) {
			assertTrue(e.toString().contains("blocked by foreign key"));
		}
		t1.ck_complete();
	}

	private void shouldBlock(Transaction t1, Record rec) {
		try {
			db.addRecord(t1, "test2", rec);
			assertTrue(false);
		} catch (SuException e) {
			assertTrue(e.toString().contains("blocked by foreign key"));
		}
	}

	@Test
	public void foreign_key_addIndex1() {
		makeTable(3);

		try {
			db.addIndex("test", "b", false, false, false, "foo", "",
					Index.BLOCK);
			assertTrue(false);
		} catch (SuException e) {
			assertTrue(e.toString().contains("blocked by foreign key"));
		}
	}

	@Test
	public void foreign_key_addIndex2() {
		makeTable(3);

		db.addTable("test2");
		db.addColumn("test2", "a");
		db.addColumn("test2", "f1");
		db.addColumn("test2", "f2");
		db.addIndex("test2", "a", true);

		Transaction t1 = db.readwriteTran();
		db.addRecord(t1, "test2", record(10, 1, 5));
		t1.ck_complete();

		db.addIndex("test2", "f1", false, false, false, "test", "a",
				Index.BLOCK);

		try {
			db.addIndex("test2", "f2", false, false, false, "test", "a",
					Index.BLOCK);
			assertTrue(false);
		} catch (SuException e) {
			assertTrue(e.toString().contains("blocked by foreign key"));
		}
	}

	@Test
	public void foreign_key_cascade_deletes() {
		makeTable(3);

		db.addTable("test2");
		db.addColumn("test2", "a");
		db.addColumn("test2", "f");
		db.addIndex("test2", "a", true);
		db.addIndex("test2", "f", false, false, false, "test", "a",
				Index.CASCADE_DELETES);

		Transaction t1 = db.readwriteTran();
		db.addRecord(t1, "test2", record(10, 1));
		db.addRecord(t1, "test2", record(11, 1));
		t1.ck_complete();

		Transaction t2 = db.readwriteTran();
		db.removeRecord(t2, "test", "a", key(1));
		t2.ck_complete();
		assertEquals(0, get("test2").size());
	}

	@Test
	public void foreign_key_cascade_updates() {
		makeTable(3);

		db.addTable("test2");
		db.addColumn("test2", "a");
		db.addColumn("test2", "f");
		db.addIndex("test2", "a", true);
		db.addIndex("test2", "f", false, false, false, "test", "a",
				Index.CASCADE_UPDATES);

		Table table = db.getTable("test");
		ForeignKey fk = table.getIndex("a").fkdsts.get(0);
		assertEquals(db.getTable("test2").num, fk.tblnum);
		assertEquals("f", fk.columns);
		assertEquals(Index.CASCADE_UPDATES, fk.mode);

		Transaction t1 = db.readwriteTran();
		db.addRecord(t1, "test2", record(10, 1));
		db.addRecord(t1, "test2", record(11, 1));
		t1.ck_complete();

		Transaction t2 = db.readwriteTran();
		db.updateRecord(t2, "test", "a", key(1), record(111));
		t2.ck_complete();
		List<Record> recs = get("test2");
		assertEquals(2, recs.size());
		assertEquals(record(10, 111), recs.get(0));
		assertEquals(record(11, 111), recs.get(1));
	}

	// TODO add/get/remove views
	// TODO remove column/index/table
	// TODO rename column/table

	@Test
	public void schema() {
		assertEquals(
				"(table,tablename,nextfield,nrows,totalsize) key(table) key(tablename)",
				db.schema("tables"));
	}

}
