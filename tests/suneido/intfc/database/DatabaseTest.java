package suneido.intfc.database;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static suneido.Suneido.dbpkg;

import java.util.List;

import org.junit.Test;

public class DatabaseTest extends TestBase {
	@Test
	public void address() {
		makeTable(1);
		assertThat(get().get(0).address(), not(is(0)));
	}

	@Test
	public void empty_table() {
		makeTable();
		Transaction t = db.readonlyTran();
		Table tbl = t.getTable("test");
		assertThat(t.tableCount(tbl.num()), is(0));
		assertThat(t.tableSize(tbl.num()), is(0L));
		assertThat(tbl.getColumns().toString(), is("[a, b]"));
		assertThat(tbl.indexesColumns().toString(), is("[[a], [b, a]]"));
		t.ck_complete();
	}

	@Test
	public void add_remove() {
		makeTable();

		Record r = dbpkg.recordBuilder().add(12).add(34).build();
		Transaction t = db.readwriteTran();
		t.addRecord("test", r);
		t.ck_complete();

		List<Record> recs = get("test");
		assertThat(recs.size(), is(1));
		assertThat(recs.get(0), is(r));
		assertThat(getNrecords("test"), is(1));

		db = db.reopen();

		recs = get("test");
		assertThat(recs.size(), is(1));
		assertThat(recs.get(0), is(r));
		assertThat(getNrecords("test"), is(1));

		t = db.readwriteTran();
		t.removeRecord(getTable("test").num(), recs.get(0));
		t.ck_complete();

		assertThat(count("test"), is(0));
		assertThat(getNrecords("test"), is(0));
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
			t.abortIfNotComplete();
		}

		t = db.readonlyTran();
		try {
			assertEquals(3, getNrecords("test"));
		} finally {
			t.ck_complete();
		}
	}


	@Test
	public void foreign_key_block() {
		// create test2 before test
		// to check that when test is created it picks up foreign keys properly
		db.createTable("test2")
			.addColumn("b")
			.addColumn("f1")
			.addColumn("f2")
			.addIndex("b", true, false, "", "", 0)
			.addIndex("f1", false, false, "test", "a", Fkmode.BLOCK)
			.addIndex("f2", false, false, "test", "a", Fkmode.BLOCK)
			.finish();
		assertThat(db.getSchema("test2"),
				is("(b,f1,f2) key(b) index(f1) in test(a) index(f2) in test(a)"));

		makeTable(3);
		List<Record> recs = get("test");

		assertRecords(0);
		Transaction t = db.readwriteTran();
		Record rec2 = record(10, 1, 2);
		t.addRecord("test2", rec2);
		t.ck_complete();

		addShouldBlock(record(11, 5, 1));
		addShouldBlock(record(11, 1, 5));
		assertRecords(1);

		removeShouldBlock(recs.get(1));
		removeShouldBlock(recs.get(2));
		assertRecords(1);

		updateShouldBlock("test", recs.get(1), record(9)); // test2 => 1
		updateShouldBlock("test2", get("test2").get(0), record(10, 1, 9));

		assertRecords(1);
	}

	private void assertRecords(int n) {
		Transaction t;
		t = db.readonlyTran();
		assertThat(t.tableCount(t.getTable("test2").num()), is(n));
		t.ck_complete();
	}

	private void addShouldBlock(Record rec) {
		Transaction t = db.readwriteTran();
		try {
			t.addRecord("test2", rec);
			fail("expected exception");
		} catch (RuntimeException e) {
			assertThat(e.toString(), containsString("blocked by foreign key"));
		}
		t.ck_complete();
	}

	private void removeShouldBlock(Record rec) {
		Transaction t = db.readwriteTran();
		Table tbl = getTable("test");
		try {
			t.removeRecord(tbl.num(), rec);
			fail("expected exception");
		} catch (RuntimeException e) {
			assertThat(e.toString(), containsString("blocked by foreign key"));
		}
		t.ck_complete();
	}

	private void updateShouldBlock(String table, Record oldrec, Record newrec) {
		Transaction t = db.readwriteTran();
		Table tbl = getTable(table);
		try {
			t.updateRecord(tbl.num(), oldrec, newrec);
			fail("expected exception");
		} catch (RuntimeException e) {
			assertThat(e.toString(), containsString("blocked by foreign key"));
		}
		t.ck_complete();
	}

	@Test
	public void foreign_key_addIndex1() {
		makeTable(3);

		try {
			db.alterTable("test")
					.addIndex("b", false, false, "foo", "a", Fkmode.BLOCK).finish();
			fail("expected exception");
		} catch (RuntimeException e) {
			assertThat(e.toString(), containsString("blocked by foreign key"));
		}
	}

	@Test
	public void foreign_key_addIndex2() {
		makeTable(3);

		db.createTable("test2")
			.addColumn("a")
			.addColumn("f1")
			.addColumn("f2")
			.addIndex("a", true, false, "", "", 0)
			.finish();

		add("test2", record(10, 1, 5));

		db.alterTable("test2")
				.addIndex("f1", false, false, "test", "a", Fkmode.BLOCK)
				.finish();

		try {
			db.alterTable("test2")
					.addIndex("f2", false, false, "test", "a", Fkmode.BLOCK)
					.finish();
			fail("expected exception");
		} catch (RuntimeException e) {
			assertThat(e.toString(), containsString("blocked by foreign key"));
		}
	}

	@Test
	public void foreign_key_cascade_deletes() {
		makeTable(3);
		make_test2(Fkmode.CASCADE_DELETES);

		List<Record> recs = get("test2");
		assertEquals(2, recs.size());
		assertEquals(record(10, 1), recs.get(0));
		assertEquals(record(11, 1), recs.get(1));

		Transaction t = db.readwriteTran();
		t.removeRecord(t.getTable("test").num(), get("test").get(1));
		t.ck_complete();
		assertEquals(0, count("test2"));
	}

	@Test
	public void foreign_key_cascade_updates() {
		makeTable(3);
		make_test2(Fkmode.CASCADE_UPDATES);

		Transaction t = db.readwriteTran();
		t.updateRecord(t.getTable("test").num(), get("test").get(1), record(111));
		t.ck_complete();
		List<Record> recs = get("test2");
		assertEquals(2, recs.size());
		assertEquals(record(10, 111), recs.get(0));
		assertEquals(record(11, 111), recs.get(1));
	}

	@Test
	public void create_index_blocked() {
		db.createTable("source")
			.addColumn("id")
			.addColumn("date")
			.addIndex("id,date", true, false, "", "", 0)
			.finish();
		add("source", record(1, 990101));
		assertThat(count("source"), is(1));
		db.createTable("target")
			.addColumn("id")
			.addColumn("name")
			.addIndex("id", true, false, "", "", 0)
			.finish();
		try {
			db.alterTable("source")
				.addIndex("id", false, false, "target", "id", Fkmode.BLOCK)
				.finish();
			fail();
		} catch (Exception e) {
			assertThat(e.toString(), containsString("blocked"));
		}
	}

	private void add(String table, Record rec) {
		Transaction t = db.readwriteTran();
		t.addRecord(table, rec);
		t.ck_complete();
	}

	private void make_test2(int fkmode) {
		db.createTable("test2")
			.addColumn("a")
			.addColumn("f")
			.addIndex("f", false, false, "test", "a", fkmode)
			.addIndex("a", true, false, "", "", 0)
			.finish();

		Transaction t1 = db.readwriteTran();
		t1.addRecord("test2", record(10, 1));
		t1.addRecord("test2", record(11, 1));
		t1.ck_complete();
	}

}
