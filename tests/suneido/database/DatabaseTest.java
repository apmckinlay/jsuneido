package suneido.database;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import suneido.SuException;
import suneido.database.Index.ForeignKey;
import suneido.util.ByteBuf;

public class DatabaseTest extends TestBase {

	@Test
	public void output_input() {
		Record r = new Record().add("hello");
		long offset = TheDb.db().output(1234, r);

		reopen();

		ByteBuf bb = TheDb.db().adr(offset - 4);
		assertEquals(1234, bb.getInt(0));

		Record r2 = TheDb.db().input(offset);
		assertEquals(r, r2);
	}

	@Test
	public void test() {
		Table tbl = TheDb.db().getTable("indexes");
		assertEquals("indexes", tbl.name);

		makeTable();

		tbl = TheDb.db().getTable("test");
		assertEquals(2, tbl.columns.size());
		assertEquals(2, tbl.indexes.size());

		Record r = new Record().add(12).add(34);
		Transaction t = TheDb.db().readwriteTran();
		t.addRecord("test", r);
		t.ck_complete();

		assertEquals(1, TheDb.db().getNrecords("test"));

		List<Record> recs = get("test");
		assertEquals(1, recs.size());
		assertEquals(r, recs.get(0));

		reopen();

		assertEquals(1, TheDb.db().getNrecords("test"));

		recs = get("test");
		assertEquals(1, recs.size());
		assertEquals(r, recs.get(0));

		t = TheDb.db().readwriteTran();
		t.removeRecord("test", "a", new Record().add(12));
		t.ck_complete();

		assertEquals(0, get("test").size());
	}

	@Test
	public void test_tabledata_for_empty_table() {
		makeTable(0);
		Transaction t = TheDb.db().readonlyTran();
		Table tbl = t.getTable("tables");
		BtreeIndex bti = t.getBtreeIndex(tbl.num, "tablename");
		Record key = new Record().add("test");
		BtreeIndex.Iter iter = bti.iter(t, key).next();
		Record rec = TheDb.db().input(iter.keyadr());
		t.ck_complete();
		assertEquals("test", rec.getString(Table.TABLE));
		assertEquals(0, rec.get(Table.NROWS));
		assertEquals(0, rec.get(Table.TOTALSIZE));
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
		TheDb.db().addColumn("test", "c");
		TheDb.db().addIndex("test", "c", false);
		Transaction t = TheDb.db().readonlyTran();
		Table table = t.getTable("test");
		Index index = table.getIndex("c");
		int i = 0;
		BtreeIndex bti = t.getBtreeIndex(table.num, index.columns);
		BtreeIndex.Iter iter = bti.iter(t).next();
		for (; !iter.eof(); iter.next())
			assertEquals(record(i++), TheDb.db().input(iter.keyadr()));
	}

	@Test
	public void duplicate_key_add() {
		makeTable(3);

		Transaction t = TheDb.db().readwriteTran();
		try {
			t.addRecord("test", record(1));
			fail("expected exception");
		} catch (SuException e) {
			assertTrue(e.toString().contains("duplicate key: a"));
		} finally {
			t.ck_complete();
		}

		t = TheDb.db().readonlyTran();
		try {
			assertEquals(3, t.getTableData(t.getTable("test").num).nrecords);
		} finally {
			t.ck_complete();
		}
	}

	@Test
	public void duplicate_key_update() {
		makeTable(3);

		Transaction t = TheDb.db().readwriteTran();
		try {
			t.updateRecord("test", "a", new Record().add(1), record(2));
			fail("expected exception");
		} catch (SuException e) {
			assertTrue(e.toString().contains("update record: duplicate key: a"));
		} finally {
			t.ck_complete();
		}

		t = TheDb.db().readonlyTran();
		try {
			assertEquals(3, t.getTableData(t.getTable("test").num).nrecords);
		} finally {
			t.ck_complete();
		}
	}

	@Test
	public void foreign_key_addRecord() {
		makeTable(3);

		TheDb.db().addTable("test2");
		TheDb.db().addColumn("test2", "b");
		TheDb.db().addColumn("test2", "f1");
		TheDb.db().addColumn("test2", "f2");
		TheDb.db().addIndex("test2", "b", true);
		TheDb.db().addIndex("test2", "f1", false, false, false, "test", "a", Index.BLOCK);
		TheDb.db().addIndex("test2", "f2", false, false, false, "test", "a", Index.BLOCK);

		Table test2 = TheDb.db().getTable("test2");
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

		Transaction t1 = TheDb.db().readwriteTran();
		t1.addRecord("test2", record(10, 1, 2));
		shouldBlock(t1, record(11, 5, 1));
		shouldBlock(t1, record(11, 1, 5));

		try {
			t1.removeRecord("test", "a", key(1));
			fail("expected exception");
		} catch (SuException e) {
			assertTrue(e.toString().contains("blocked by foreign key"));
		}
		t1.ck_complete();
	}

	private void shouldBlock(Transaction t1, Record rec) {
		try {
			t1.addRecord("test2", rec);
			fail("expected exception");
		} catch (SuException e) {
			assertTrue(e.toString().contains("blocked by foreign key"));
		}
	}

	@Test
	public void foreign_key_addIndex1() {
		makeTable(3);

		try {
			TheDb.db().addIndex("test", "b", false, false, false, "foo", "", Index.BLOCK);
			fail("expected exception");
		} catch (SuException e) {
			assertTrue(e.toString().contains("blocked by foreign key"));
		}
	}

	@Test
	public void foreign_key_addIndex2() {
		makeTable(3);

		TheDb.db().addTable("test2");
		TheDb.db().addColumn("test2", "a");
		TheDb.db().addColumn("test2", "f1");
		TheDb.db().addColumn("test2", "f2");
		TheDb.db().addIndex("test2", "a", true);

		Transaction t1 = TheDb.db().readwriteTran();
		t1.addRecord("test2", record(10, 1, 5));
		t1.ck_complete();

		TheDb.db().addIndex("test2", "f1", false, false, false, "test", "a",
				Index.BLOCK);

		try {
			TheDb.db().addIndex("test2", "f2", false, false, false, "test", "a",
					Index.BLOCK);
			fail("expected exception");
		} catch (SuException e) {
			assertTrue(e.toString().contains("blocked by foreign key"));
		}
	}

	@Test
	public void foreign_key_cascade_deletes() {
		makeTable(3);

		TheDb.db().addTable("test2");
		TheDb.db().addColumn("test2", "a");
		TheDb.db().addColumn("test2", "f");
		TheDb.db().addIndex("test2", "a", true);
		TheDb.db().addIndex("test2", "f", false, false, false, "test", "a",
				Index.CASCADE_DELETES);

		Transaction t1 = TheDb.db().readwriteTran();
		t1.addRecord("test2", record(10, 1));
		t1.addRecord("test2", record(11, 1));
		t1.ck_complete();

		Transaction t2 = TheDb.db().readwriteTran();
		t2.removeRecord("test", "a", key(1));
		t2.ck_complete();
		assertEquals(0, get("test2").size());
	}

	@Test
	public void foreign_key_cascade_updates() {
		makeTable(3);

		TheDb.db().addTable("test2");
		TheDb.db().addColumn("test2", "a");
		TheDb.db().addColumn("test2", "f");
		TheDb.db().addIndex("test2", "a", true);
		TheDb.db().addIndex("test2", "f", false, false, false, "test", "a",
				Index.CASCADE_UPDATES);

		Table table = TheDb.db().getTable("test");
		ForeignKey fk = table.getIndex("a").fkdsts.get(0);
		assertEquals(TheDb.db().getTable("test2").num, fk.tblnum);
		assertEquals("f", fk.columns);
		assertEquals(Index.CASCADE_UPDATES, fk.mode);

		Transaction t1 = TheDb.db().readwriteTran();
		t1.addRecord("test2", record(10, 1));
		t1.addRecord("test2", record(11, 1));
		t1.ck_complete();

		Transaction t2 = TheDb.db().readwriteTran();
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
				TheDb.db().getTable("tables").schema());
	}

}
