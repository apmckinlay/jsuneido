/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static suneido.database.immudb.TestBase.rec;

import org.junit.After;
import org.junit.Test;

import suneido.SuException;

//TODO use TestBase

public class TransactionTest {
	private final Storage stor = new HeapStorage();
	private final Storage istor = new HeapStorage();
	private Database db = Database.create("", stor, istor);

	@Test
	public void create() {
		Persist.persist(db);
		db.checkTransEmpty();
	}

	@Test
	public void check_empty_commit() {
		UpdateTransaction t = db.updateTransaction();
		t.onlyReads = false; // force commit output
		t.commit();
	}

	@Test
	public void check_one_add() {
		make_tmp();
		UpdateTransaction t = db.updateTransaction();
		assertThat(t.readCount(), equalTo(0));
		assertThat(t.writeCount(), equalTo(0));
		t.addRecord("tmp", rec(123, "foo"));
		assertThat(t.readCount(), equalTo(0));
		assertThat(t.writeCount(), equalTo(1));
		t.commit();
	}

	@Test
	public void check_multiple_commits() {
		make_tmp();
		UpdateTransaction t = db.updateTransaction();
		t.addRecord("tmp", rec(123, "foo"));
		t.commit();
		t = db.updateTransaction();
		t.addRecord("tmp", rec(456, "bar"));
		t.commit();
	}

	@Test
	public void lookup() {
		Database db = Dbpkg.testdb();
		Transaction t = db.readTransaction();
		Record key = new RecordBuilder().add("indexes").build();
		Record r = t.lookup(1, "tablename", key);
		assertThat(r.getString(1), equalTo("indexes"));

		key = new RecordBuilder().add("fred").build();
		r = t.lookup(1, "tablename", key);
		assertNull(r);
	}

	@Test
	public void read_tables() {
		db.checkTransEmpty();
		db = db.reopen();
		check(db.readTransaction());
		check(db.updateTransaction());
		check(db.bulkTransaction());
		db.checkTransEmpty();
		db.close();
	}
	private static void check(ReadTransaction t) {
		Record[] recs = { rec(1, "tables"), rec(2, "columns"), rec(3, "indexes"),
				rec(4, "views") };
		check(t, Bootstrap.TN.TABLES, recs);
	}

	@Test
	public void add_remove() {
		make_tmp();

		UpdateTransaction t = db.updateTransaction();
		int tblnum = t.getTable("tmp").num;
		assertThat(t.tableCount(tblnum), equalTo(0));
		assertThat(t.tableSize(tblnum), equalTo(0L));
		t.addRecord("tmp", rec(123, "foo"));
		assertThat(t.tableCount(tblnum), equalTo(1));
		assertThat(t.tableSize(tblnum), equalTo(13L));
		assertNotNull(t.lookup(tblnum, new int[] { 0 }, rec(123)));
		check(t, "tmp", rec(123, "foo"));
		t = null;

		db.checkTransEmpty();
		db = db.reopen();
		check(db.readTransaction(), "tmp", rec(123, "foo"));

		ReadTransaction rt = db.readTransaction();
		assertThat(rt.tableCount(tblnum), equalTo(1));
		assertThat(rt.tableSize(tblnum), equalTo(13L));
		DataRecord r = rt.lookup(tblnum, new int[] { 0 }, rec(123));
		assertThat(r, equalTo(rec(123, "foo")));
		rt.complete();
		rt = null;

		check(db.readTransaction(), "tmp", rec(123, "foo"));

		t = db.updateTransaction();
		r = t.lookup(tblnum, new int[] { 0 }, rec(123));
		t.updateRecord(tblnum, r, rec(123, "foo"));
		check(t, "tmp", rec(123, "foo"));
		t = null;

		t = db.updateTransaction();
		r = t.lookup(tblnum, new int[] { 0 }, rec(123));
		t.removeRecord(tblnum, r);
		assertThat(t.tableCount(tblnum), equalTo(0));
		assertThat(t.tableSize(tblnum), equalTo(0L));
		assertNull(t.lookup(tblnum, new int[] { 0 }, rec(123)));
		check(t, "tmp");
		t = null;

		t = db.updateTransaction();
		assertThat(t.tableCount(tblnum), equalTo(0));
		assertThat(t.tableSize(tblnum), equalTo(0L));
		t.addRecord(tblnum, rec(456, "bar"));
		r = t.lookup(tblnum, new int[] { 0 }, rec(456));
		t.removeRecord(tblnum, r);
		check(t, "tmp");
		t = null;

		check(db.readTransaction(), "tmp");
		db.checkTransEmpty();
		db.close();
	}

	@Test
	public void non_unique_index() {
		db.createTable("test2")
			.addColumn("a")
			.addColumn("f")
			.addIndex("a", true, false, "", "", 0)
			.addIndex("f", false, false, "", "", 0)
			.finish();

		Transaction t = db.updateTransaction();
		int tblnum = t.getTable("test2").num();
		t.addRecord("test2", rec(10, 1));
		t.addRecord("test2", rec(11, 1));
		t.ck_complete();

		check(db.readTransaction(), tblnum, "f", rec(10, 1), rec(11, 1));
	}

	@Test
	public void duplicate_abort_during_commit() {
		make_tmp();

		UpdateTransaction t = db.updateTransaction();
		t.addRecord("tmp", rec(123, "foo"));
		try {
			t.addRecord("tmp", rec(123, "bar")); // within same transaction
			fail();
		} catch (RuntimeException e) {
			assertThat(e.toString(), containsString("duplicate"));
		}
		t.ck_complete();

		t = db.updateTransaction();
		try {
			t.addRecord("tmp", rec(123, "bar")); // in a separate transaction
			fail();
		} catch (RuntimeException e) {
			assertThat(e.toString(), containsString("duplicate"));
		}
		t.ck_complete();

		// duplicate while updating btrees in commit
		// data commit will need to be aborted
		t = db.updateTransaction();
		UpdateTransaction t2 = db.updateTransaction();
		t.addRecord("tmp", rec(456, "foo"));
		t2.addRecord("tmp", rec(456, "bar"));
		t.ck_complete();
		assertThat(t2.complete(), containsString("duplicate"));
	}

	@Test
	public void views() {
		db.addView("myview", "tables join columns");
		assertThat(db.getView("myview"), equalTo("tables join columns"));
	}

	@Test
	public void delete_visibility() {
		make_tmp();

		UpdateTransaction t = db.updateTransaction();
		int tmp = t.getTable("tmp").num();
		t.addRecord("tmp", rec(123, "foo"));
		t.ck_complete();

		UpdateTransaction t1 = db.updateTransaction();
		UpdateTransaction t2 = db.updateTransaction();
		t1.removeRecord(tmp, rec(123, "foo"));
		t1.ck_complete();
		assertThat(t2.lookup(tmp, "a", rec(123)), equalTo(rec(123, "foo")));
		t2.abort();
	}

	@Test
	public void concurrent_appends() {
		make_tmp();

		Transaction t1 = db.updateTransaction();
		t1.addRecord("tmp", rec(123, "foo"));
		Transaction t2 = db.updateTransaction();
		t2.addRecord("tmp", rec(456, "bar"));
		t1.ck_complete();
		t2.ck_complete();
	}

	@Test
	public void delete_conflict() {
		make_tmp();

		Transaction t = db.updateTransaction();
		int tmp = t.getTable("tmp").num();
		t.addRecord("tmp", rec(123, "foo"));
		t.ck_complete();

		Transaction t1 = db.updateTransaction();
		Transaction t2 = db.updateTransaction();
		t1.removeRecord(tmp, rec(123, "foo"));
		t2.removeRecord(tmp, rec(123, "foo"));
		t1.ck_complete();
		assertThat(t2.complete(), containsString("transaction conflict: delete"));
	}

	// successful read conflicting with delete
	@Test
	public void read_validation_for_delete() {
		make_tmp();

		Transaction t = db.updateTransaction();
		int tmp = t.getTable("tmp").num();
		t.addRecord("tmp", rec(123, "foo"));
		t.ck_complete(); t = null;

		UpdateTransaction t1 = db.updateTransaction();
		t1.onlyReads = false;
		assertThat(t1.lookup(tmp, "a", rec(123)), equalTo(rec(123, "foo")));

		UpdateTransaction t2 = db.updateTransaction();
		t2.removeRecord(tmp, rec(123, "foo"));
		t2.ck_complete();

		assertThat(t1.complete(), containsString("transaction conflict: read"));
	}

	// unsuccessful read conflicting with add
	@Test
	public void read_validation_for_add() {
		make_tmp();

		UpdateTransaction t1 = db.updateTransaction();
		t1.onlyReads = false;
		int tmp = t1.getTable("tmp").num();
		assertNull(t1.lookup(tmp, "a", rec(123)));

		UpdateTransaction t2 = db.updateTransaction();
		t2.addRecord("tmp", rec(123, "foo"));
		t2.ck_complete();

		assertThat(t1.complete(), containsString("transaction conflict: read"));
	}

	@Test
	public void empty_bulk_transaction() {
		BulkTransaction t = db.bulkTransaction();
		t.ck_complete();
	}

	@Test
	public void abort_empty_bulk_transaction() {
		BulkTransaction t = db.bulkTransaction();
		t.abort();
	}

	@Test
	public void bulk_transaction() {
		make_tmp(4);
		BulkTransaction t = db.bulkTransaction();
		Table tbl = t.getTable("tmp");
		int first = t.loadRecord(tbl.num, rec(123, "foo"));
		int last = t.loadRecord(tbl.num, rec(456, "bar"));
		DbLoad.createIndexes(t, tbl, first, last);
		t.ck_complete();
	}

	@Test
	public void abort_bulk_transaction1() {
		make_tmp(4);
		BulkTransaction t = db.bulkTransaction();
		Table tbl = t.getTable("tmp");
		t.loadRecord(tbl.num, rec(123, "foo"));
		t.abort();
	}

	@Test
	public void abort_bulk_transaction2() {
		make_tmp(4);
		BulkTransaction t = db.bulkTransaction();
		Table tbl = t.getTable("tmp");
		int first = t.loadRecord(tbl.num, rec(123, "foo"));
		int last = t.loadRecord(tbl.num, rec(456, "bar"));
		DbLoad.createIndexes(t, tbl, first, last);
		t.abort();
	}

	@Test
	public void add_delete_column() {
		make_tmp(100);
		db.ensureTable("tmp").addColumn("foo").finish();
		db.alterTable("tmp").dropColumn("foo").finish();
		db.ensureTable("tmp").addColumn("foo").finish();
		db.alterTable("tmp").dropColumn("foo").finish();
	}

	@Test
	public void update_during_exclusive() {
		var st = db.schemaTransaction();
		st.exclusive();
		try {
			db.updateTransaction();
			fail("expected exception");
		} catch (SuException se) {
			assertEquals("blocked by exclusive transaction", se.toString());
		}
		assert !st.ended;
	}

	//--------------------------------------------------------------------------

	@After
	public void check() {
		assert "".equals(db.check());
	}

	private void make_tmp(int nrecs) {
		make_tmp();
		UpdateTransaction t = db.updateTransaction();
		for (int i = 0; i < nrecs; ++i)
			t.addRecord("tmp", rec(i, i));
		t.ck_complete();
	}

	private void make_tmp() {
		db.createTable("tmp")
			.addColumn("a")
			.addColumn("b")
			.addIndex("a", true, false, null, null, 0)
			.finish();
	}

	private static void check(ReadTransaction t, String tableName, Record... recs) {
		Table tbl = t.getTable(tableName);
		check(t, tbl.num, recs);
	}

	private static void check(ReadTransaction t, int tblnum, Record... recs) {
		check(t, tblnum, null, recs);
	}

	private static void check(ReadTransaction t, int tblnum, String columns,
			Record... recs) {
		int i = 0;
		IndexIter iter = t.iter(tblnum, columns);
		for (iter.next(); ! iter.eof(); iter.next(), ++i)
			assertThat(t.input(iter.keyadr()), equalTo(recs[i]));
		assertThat(i, equalTo(recs.length));
		t.ck_complete();
	}

}
