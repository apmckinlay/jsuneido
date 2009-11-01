package suneido.database;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import suneido.SuException;
import suneido.database.Index.ForeignKey;

public class DatabaseTest extends TestBase {

//	@Test
//	public void output_input() {
//		Record r = new Record().add("hello");
//		long offset = db.output(1234, r);
//
//		reopen();
//
//		ByteBuf bb = db.adr(offset - 4);
//		assertEquals(1234, bb.getInt(0));
//
//		Record r2 = db.input(offset);
//		assertEquals(r, r2);
//	}

	@Test
	public void test() {
		Table tbl = db.tables.get("indexes");
		assertEquals("indexes", tbl.name);
		assertSame(tbl, db.tables.get(3));

		makeTable();

		tbl = db.tables.get("test");
		assertEquals(2, tbl.columns.size());
		assertEquals(2, tbl.indexes.size());

		Record r = new Record().add(12).add(34);
		Transaction t = db.readwriteTran();
		t.addRecord("test", r);
		t.ck_complete();

		assertEquals(1, db.getNrecords("test"));

		List<Record> recs = get("test");
		assertEquals(1, recs.size());
		assertEquals(r, recs.get(0));

		reopen();

		assertEquals(1, db.getNrecords("test"));

		recs = get("test");
		assertEquals(1, recs.size());
		assertEquals(r, recs.get(0));

		t = db.readwriteTran();
		t.removeRecord("test", "a", new Record().add(12));
		t.ck_complete();

		assertEquals(0, get("test").size());
	}

	@Test
	public void test_multi_node_index() {
		final int N = 2000;
		makeTable(N);
		List<Record> recs = get("test");
		assertEquals(N, recs.size()); // TODO don't use get
	}

	// add index to table with existing records
	@Test
	public void add_index() {
		makeTable(3);
		db.addColumn("test", "c");
		db.addIndex("test", "c", false);
		Transaction t = db.readonlyTran();
		Table table = t.getTable("test");
		Index index = table.getIndex("c");
		int i = 0;
		BtreeIndex bti = t.getBtreeIndex(table.num, index.columns);
		BtreeIndex.Iter iter = bti.iter(t).next();
		for (; !iter.eof(); iter.next())
			assertEquals(record(i++), db.input(iter.keyadr()));
	}

	@Test
	public void duplicate_key() {
		makeTable(3);

		Transaction t = db.readwriteTran();
		try {
			t.addRecord("test", record(1));
		} catch (SuException e) {
			assertTrue(e.toString().contains("duplicate key: a"));
		}
		t.ck_complete();
	}

	@Test
	public void foreign_key_addRecord() {
		makeTable(3);

		db.addTable("test2");
		db.addColumn("test2", "b");
		db.addColumn("test2", "f1");
		db.addColumn("test2", "f2");
		db.addIndex("test2", "b", true);
		db.addIndex("test2", "f1", false, false, false, "test", "a", Index.BLOCK);
		db.addIndex("test2", "f2", false, false, false, "test", "a", Index.BLOCK);

		Table test2 = db.tables.get("test2");
		Index f1 = test2.indexes.get("f1");
		assertEquals("f1", f1.columns);
		assertEquals(1, (int) f1.colnums.get(0));
		ForeignKey fk = f1.fksrc;
		assertEquals("test", fk.tablename);
		assertEquals("a", fk.columns);
		assertEquals(0, fk.mode);
		Index f2 = test2.indexes.get("f2");
		assertEquals("f2", f2.columns);
		assertEquals(2, (int) f2.colnums.get(0));
		fk = f2.fksrc;
		assertEquals("test", fk.tablename);
		assertEquals("a", fk.columns);
		assertEquals(0, fk.mode);

		Transaction t1 = db.readwriteTran();
		t1.addRecord("test2", record(10, 1, 2));
		shouldBlock(t1, record(11, 5, 1));
		shouldBlock(t1, record(11, 1, 5));

		try {
			t1.removeRecord("test", "a", key(1));
			assertTrue(false);
		} catch (SuException e) {
			assertTrue(e.toString().contains("blocked by foreign key"));
		}
		t1.ck_complete();
	}

	private void shouldBlock(Transaction t1, Record rec) {
		try {
			t1.addRecord("test2", rec);
			assertTrue(false);
		} catch (SuException e) {
			assertTrue(e.toString().contains("blocked by foreign key"));
		}
	}

	@Test
	public void foreign_key_addIndex1() {
		makeTable(3);

		try {
			db.addIndex("test", "b", false, false, false, "foo", "", Index.BLOCK);
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
		t1.addRecord("test2", record(10, 1, 5));
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
		t1.addRecord("test2", record(10, 1));
		t1.addRecord("test2", record(11, 1));
		t1.ck_complete();

		Transaction t2 = db.readwriteTran();
		t2.removeRecord("test", "a", key(1));
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

		Table table = db.tables.get("test");
		ForeignKey fk = table.getIndex("a").fkdsts.get(0);
		assertEquals(db.tables.get("test2").num, fk.tblnum);
		assertEquals("f", fk.columns);
		assertEquals(Index.CASCADE_UPDATES, fk.mode);

		Transaction t1 = db.readwriteTran();
		t1.addRecord("test2", record(10, 1));
		t1.addRecord("test2", record(11, 1));
		t1.ck_complete();

		Transaction t2 = db.readwriteTran();
		t2.updateRecord("test", "a", key(1), record(111));
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
