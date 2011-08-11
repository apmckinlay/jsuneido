package suneido.intfc.database;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static suneido.Suneido.dbpkg;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class DatabaseTest {
	Database db = dbpkg.testdb();

	@Test
	public void address() {
		makeTable(1);
		assertThat(get().get(0).address(), not(is(0)));
	}

	@Test
	public void empty_table() {
		makeTable(0);
		Transaction t = db.readonlyTran();
		Table tbl = t.getTable("test");
		assertThat(t.tableCount(tbl.num()), is(0));
		assertThat(t.tableSize(tbl.num()), is(0L));
		t.ck_complete();
	}

	@Test
	public void test() {
		Table tbl = getTable("indexes");
		assertEquals("indexes", tbl.name());

		makeTable();

		tbl = getTable("test");
		assertEquals(2, tbl.getColumns().size());
		assertEquals(2, tbl.indexesColumns().size());

		Record r = dbpkg.recordBuilder().add(12).add(34).build();
		Transaction t = db.readwriteTran();
		t.addRecord("test", r);
		t.ck_complete();

		assertEquals(1, getNrecords("test"));

		List<Record> recs = get("test");
		assertEquals(1, recs.size());
		assertEquals(r, recs.get(0));

		db = db.reopen();

		assertEquals(1, getNrecords("test"));

		recs = get("test");
		assertEquals(1, recs.size());
		assertEquals(r, recs.get(0));

		t = db.readwriteTran();
		t.removeRecord(getTable("test").num(), recs.get(0));
		t.ck_complete();

		assertEquals(0, get("test").size());
	}

	Table getTable(String tableName) {
		Transaction t = db.readonlyTran();
		Table tbl = t.getTable(tableName);
		t.complete();
		return tbl;
	}

	int getNrecords(String tableName) {
		Transaction t = db.readonlyTran();
		Table tbl = t.getTable(tableName);
		int n = t.tableCount(tbl.num());
		t.complete();
		return n;
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
		TableBuilder tb = db.createTable(tablename);
		tb.addColumn("a");
		tb.addColumn("b");
		tb.addIndex("a", true, false, "", "", 0);
		tb.addIndex("b,a", false, false, "", "", 0);
		tb.finish();

		addRecords(tablename, 0, nrecords - 1);
	}

	protected void addRecords(String tablename, int from, int to) {
		while (from <= to) {
			Transaction t = db.readwriteTran();
			for (int i = 0; i < 1000 && from <= to; ++i, ++from)
				t.addRecord(tablename, record(from));
			t.ck_complete();
		}
	}

	protected static Record record(int i) {
		return dbpkg.recordBuilder().add(i).add("more stuff").build();
	}

	protected static Record key(int i) {
		return dbpkg.recordBuilder().add(i).build();
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
		Table tbl = tran.getTable(tablename);
		IndexIter iter = tran.iter(tbl.num(), null);
		for (iter.next(); ! iter.eof(); iter.next())
			recs.add(tran.input(iter.keyadr()));
		return recs;
	}

	protected int count(String tablename) {
		Transaction tran = db.readonlyTran();
		int n = count(tablename, tran);
		tran.ck_complete();
		return n;
	}

	protected int count(String tablename, Transaction tran) {
		int n = 0;
		Table tbl = tran.getTable(tablename);
		IndexIter iter = tran.iter(tbl.num(), null);
		for (iter.next(); ! iter.eof(); iter.next())
			n++;
		return n;
	}

	@Test
	public void test_multi_node_index() {
		final int N = 2000;
		makeTable(N);
		assertThat(getNrecords("test"), is(N));
		assertThat(count("test"), is(N));
	}

	@Test
	public void add_index_to_existing_table() {
		makeTable(3);
		TableBuilder tb = db.alterTable("test");
		tb.addColumn("c");
		tb.addIndex("c", false, false, null, null, 0);
		tb.finish();
		Transaction t = db.readonlyTran();
		Table table = t.getTable("test");
		int i = 0;
		IndexIter iter = t.iter(table.num(), "c");
		for (iter.next(); ! iter.eof(); iter.next())
			assertEquals(record(i++), t.input(iter.keyadr()));
		t.ck_complete();
	}

	@Test
	public void duplicate_key_add() {
		makeTable(3);

		Transaction t = db.readwriteTran();
		try {
			t.addRecord("test", record(1));
			fail("expected exception");
		} catch (RuntimeException e) {
			assertThat(e.toString(), containsString("duplicate key"));
		} finally {
			t.ck_complete();
		}

		t = db.readonlyTran();
		try {
			assertEquals(3, getNrecords("test"));
		} finally {
			t.ck_complete();
		}
	}

	@Test
	public void duplicate_key_update() {
		makeTable(3);

		Transaction t = db.readwriteTran();
		List<Record> recs = get(t);
		try {
			t.updateRecord(t.getTable("test").num(), recs.get(1), record(2));
			fail("expected exception");
		} catch (RuntimeException e) {
			assertThat(e.toString(), containsString("duplicate key"));
		} finally {
			t.ck_complete();
		}

		t = db.readonlyTran();
		try {
			assertEquals(3, getNrecords("test"));
		} finally {
			t.ck_complete();
		}
	}

/*
	@Test
	public void foreign_key_addRecord() {
		makeTable(3);

		db.addTable("test2");
		db.addColumn("test2", "b");
		db.addColumn("test2", "f1");
		db.addColumn("test2", "f2");
		db.addIndex("test2", "b", true);
		db.addIndex("test2", "f1", false, false, "test", "a", Fkmode.BLOCK);
		db.addIndex("test2", "f2", false, false, "test", "a", Fkmode.BLOCK);

		Table test2 = db.getTable("test2");
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
			db.addIndex("test", "b", false, false, "foo", "", Fkmode.BLOCK);
			fail("expected exception");
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

		db.addIndex("test2", "f1", false, false, "test", "a",
				Fkmode.BLOCK);

		try {
			db.addIndex("test2", "f2", false, false, "test", "a",
					Fkmode.BLOCK);
			fail("expected exception");
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
		db.addIndex("test2", "f", false, false, "test", "a",
				Fkmode.CASCADE_DELETES);

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
		db.addIndex("test2", "f", false, false, "test", "a",
				Fkmode.CASCADE_UPDATES);

		Table table = db.getTable("test");
		ForeignKey fk = table.getIndex("a").fkdsts.get(0);
		assertEquals(db.getTable("test2").num, fk.tblnum);
		assertEquals("f", fk.columns);
		assertEquals(Fkmode.CASCADE_UPDATES, fk.mode);

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
*/
}
