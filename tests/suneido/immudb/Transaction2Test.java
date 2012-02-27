/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import suneido.intfc.database.IndexIter;
import suneido.intfc.database.Transaction;

public class Transaction2Test {
	private final Storage stor = new MemStorage(64, 1024);
	private final Storage istor = new MemStorage(1024, 1024);
	private Database2 db = Database2.create(stor, istor);

	@Test
	public void create() {
		//DumpData.dump(stor);
		Persist.persist(db);
		db.checkTransEmpty();
	}

	@Test
	public void read_tables() {
		db.checkTransEmpty();
		db = db.reopen();
		check(db.readonlyTran());
		check(db.readwriteTran());
		check(db.exclusiveTran());
		db.checkTransEmpty();
		db.close();
	}
	private static void check(ImmuReadTran t) {
		Record[] recs = { rec(1, "tables"), rec(2, "columns"), rec(3, "indexes"),
				rec(4, "views") };
		check(t, Bootstrap.TN.TABLES, recs);
	}

	@Test
	public void add_remove() {
		db.createTable("tmp")
				.addColumn("a")
				.addColumn("b")
				.addIndex("a", true, false, null, null, 0)
				.finish();

		ImmuUpdateTran t = db.readwriteTran();
		int tblnum = t.getTable("tmp").num;
		assertThat(t.tableCount(tblnum), is(0));
		assertThat(t.tableSize(tblnum), is(0L));
		t.addRecord("tmp", rec(123, "foo"));
		assertThat(t.tableCount(tblnum), is(1));
		assertThat(t.tableSize(tblnum), is(15L));
		assertNotNull(t.lookup(tblnum, new int[] { 0 }, rec(123)));
		check(t, "tmp", rec(123, "foo"));
		t = null;

		db.checkTransEmpty();
		db = db.reopen();
		check(db.readonlyTran(), "tmp", rec(123, "foo"));

		ImmuReadTran rt = db.readonlyTran();
		assertThat(rt.tableCount(tblnum), is(1));
		assertThat(rt.tableSize(tblnum), is(15L));
		Record r = rt.lookup(tblnum, new int[] { 0 }, rec(123));
		assertThat(r, is(rec(123, "foo")));
		rt.complete();
		rt = null;

		check(db.readonlyTran(), "tmp", rec(123, "foo"));

		t = db.readwriteTran();
		r = t.lookup(tblnum, new int[] { 0 }, rec(123));
		t.updateRecord(tblnum, r, rec(123, "foo"));
		check(t, "tmp", rec(123, "foo"));
		t = null;

		t = db.readwriteTran();
		r = t.lookup(tblnum, new int[] { 0 }, rec(123));
		t.removeRecord(tblnum, r);
		assertThat(t.tableCount(tblnum), is(0));
		assertThat(t.tableSize(tblnum), is(0L));
		assertNull(t.lookup(tblnum, new int[] { 0 }, rec(123)));
		check(t, "tmp");
		t = null;

		t = db.readwriteTran();
		assertThat(t.tableCount(tblnum), is(0));
		assertThat(t.tableSize(tblnum), is(0L));
		t.addRecord(tblnum, rec(456, "bar"));
		r = t.lookup(tblnum, new int[] { 0 }, rec(456));
		t.removeRecord(tblnum, r);
		check(t, "tmp");
		t = null;

		check(db.readonlyTran(), "tmp");
		//DumpData.dump(stor);
		db.checkTransEmpty();
		db.close();
	}

	@Test
	public void test_non_unique_index() {
		db.createTable("test2")
			.addColumn("a")
			.addColumn("f")
			.addIndex("a", true, false, "", "", 0)
			.addIndex("f", false, false, "", "", 0)
			.finish();

		Transaction t = db.readwriteTran();
		int tblnum = t.getTable("test2").num();
		t.addRecord("test2", rec(10, 1));
		t.addRecord("test2", rec(11, 1));
		t.ck_complete();

		check(db.readonlyTran(), tblnum, "f", rec(10, 1), rec(11, 1));
	}

	@Test
	public void test_duplicates() {
		db.createTable("tmp")
		.addColumn("a")
		.addColumn("b")
		.addIndex("a", true, false, null, null, 0)
		.finish();

		ImmuUpdateTran t = db.readwriteTran();
		t.addRecord("tmp", rec(123, "foo"));
		try {
			t.addRecord("tmp", rec(123, "bar")); // within same transaction
			fail();
		} catch (RuntimeException e) {
			assertThat(e.toString(), containsString("duplicate"));
		}
		t.ck_complete();

		t = db.readwriteTran();
		try {
			t.addRecord("tmp", rec(123, "bar")); // in a separate transaction
			fail();
		} catch (RuntimeException e) {
			assertThat(e.toString(), containsString("duplicate"));
		}
		t.ck_complete();

		t = db.readwriteTran();
		ImmuUpdateTran t2 = db.readwriteTran();
		t.addRecord("tmp", rec(456, "foo"));
		t2.addRecord("tmp", rec(456, "bar"));
		t.ck_complete();
		assertThat(t2.complete(), containsString("duplicate"));
	}

	@Test
	public void test_duplicates_exclusive() {
		db.createTable("tmp")
		.addColumn("a")
		.addColumn("b")
		.addIndex("a", true, false, null, null, 0)
		.finish();

		ImmuUpdateTran t = db.exclusiveTran();
		t.addRecord("tmp", rec(123, "foo"));
		try {
			t.addRecord("tmp", rec(123, "bar")); // within same transaction
			fail();
		} catch (RuntimeException e) {
			assertThat(e.toString(), containsString("duplicate"));
		}
		t.ck_complete();

		t = db.exclusiveTran();
		try {
			t.addRecord("tmp", rec(123, "bar")); // in a separate transaction
			fail();
		} catch (RuntimeException e) {
			assertThat(e.toString(), containsString("duplicate"));
		}
		t.ck_complete();
	}

	@Test
	public void test_views() {
		db.addView("myview", "tables join columns");
		assertThat(db.getView("myview"), is("tables join columns"));
	}

	private static void check(ImmuReadTran t, String tableName, Record... recs) {
		Table tbl = t.getTable(tableName);
		check(t, tbl.num, recs);
	}

	private static void check(ImmuReadTran t, int tblnum, Record... recs) {
		check(t, tblnum, null, recs);
	}

	private static void check(ImmuReadTran t, int tblnum, String columns,
			Record... recs) {
		int i = 0;
		IndexIter iter = t.iter(tblnum, columns);
		for (iter.next(); ! iter.eof(); iter.next(), ++i)
			assertThat(t.input(iter.keyadr()), is(recs[i]));
		assertThat(i, is(recs.length));
		t.complete();
	}

	static Record rec(Object... values) {
		RecordBuilder rb = new RecordBuilder();
		for (Object val : values)
			if (val instanceof Integer)
				rb.add(((Integer) val).intValue());
			else
				rb.add(val);
		return rb.build();
	}

}
