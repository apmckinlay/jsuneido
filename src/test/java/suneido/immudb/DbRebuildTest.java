/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import suneido.intfc.database.IndexIter;

/** immudb specific tests in addition to suneido.intfc.database.DbRebuildTest */
public class DbRebuildTest extends TestBase {

	@Test
	public void duplicate_abort_during_commit() {
		makeTable(4);

		// duplicate while updating btrees in commit
		// data commit will need to be aborted
		UpdateTransaction t1 = updateTransaction();
		UpdateTransaction t2 = updateTransaction();
		t1.addRecord("test", record(4));
		t2.addRecord("test", record(4));
		t1.ck_complete();
		assertThat(t2.complete(), containsString("duplicate"));

		rebuild(0, 1, 2, 3, 4);
	}

	@Test
	public void bulk_transaction() {
		makeTable(4);
		BulkTransaction t = bulkTransaction();
		Table tbl = t.getTable("test");
		int first = t.loadRecord(tbl.num, rec(4, "more stuff"));
		int last = t.loadRecord(tbl.num, rec(5, "more stuff"));
		DbLoad.createIndexes(t, tbl, first, last);
		t.ck_complete();

		rebuild(0, 1, 2, 3, 4, 5);
	}

	@Test
	public void bulk_abort() {
		makeTable(4);
		BulkTransaction t = bulkTransaction();
		Table tbl = t.getTable("test");
		int first = t.loadRecord(tbl.num, rec(4, "more stuff"));
		int last = t.loadRecord(tbl.num, rec(5, "more stuff"));
		DbLoad.createIndexes(t, tbl, first, last);
		t.abort();

		rebuild(0, 1, 2, 3);
	}

	@Test
	public void rule_columns() {
		db.createTable("test")
			.addColumn("a")
			.addColumn("Rule")
			.addColumn("b")
			.addColumn("c")
			.addIndex("c", true, false, "", "", 0)
			.finish();
		db.alterTable("test").dropColumn("b").finish();
		rebuild();
	}

	@Test
	public void rename_column() {
		makeTable(2);
		db.alterTable("test").renameColumn("b", "bb").finish();
		addRecords("test", 2, 3);
		rebuild(0, 1, 2, 3);
	}

	@Test
	public void next_table_num() {
		makeTable(2);
		db.dropTable("test");
		db = db.reopen();
		makeTable(2);
		rebuild(0, 1);
	}

	@Test
	public void new_remove() {
		makeTable();
		SpyUpdateTransaction t = spyUpdateTransaction();
		addNew(t);
		Table tbl = t.getTable("test");
		IndexIter iter = t.iter(tbl.num, null);
		for (iter.next(); ! iter.eof(); iter.next()) {
			int adr = iter.keyadr();
			t.removeRecord(adr);
		}
		t.ck_complete();
		assertThat(t.counts(), is("+0 -0 u0"));
		rebuild();
	}

	protected void addNew(SpyUpdateTransaction t) {
		t.addRecord("test", record(123));
		t.addRecord("test", record(456));
		t.addRecord("test", record(789));
	}

	@Test
	public void new_single_update() {
		makeTable();
		SpyUpdateTransaction t = spyUpdateTransaction();
		addNew(t);
		Table tbl = t.getTable("test");
		int i = 10;
		IndexIter iter = t.iter(tbl.num, null);
		for (iter.next(); ! iter.eof(); iter.next()) {
			int adr = iter.keyadr();
			t.updateRecord(adr, record(i++));
		}
		t.ck_complete();
		assertThat(t.counts(), is("+3 -0 u0"));
		rebuild(10, 11, 12);
	}

	@Test
	public void old_single_update() {
		makeTable();
		addOld();

		SpyUpdateTransaction t = spyUpdateTransaction();
		Table tbl = t.getTable("test");
		int i = 10;
		IndexIter iter = t.iter(tbl.num, null);
		for (iter.next(); ! iter.eof(); iter.next()) {
			int adr = iter.keyadr();
			t.updateRecord(adr, record(i++));
		}
		t.ck_complete();
		assertThat(t.counts(), is("+0 -0 u3"));
		rebuild(10, 11, 12);
	}

	protected void addOld() {
		UpdateTransaction t = updateTransaction();
		t.addRecord("test", record(123));
		t.addRecord("test", record(456));
		t.addRecord("test", record(789));
		t.ck_complete();
	}

	@Test
	public void new_multiple_update() {
		makeTable();
		SpyUpdateTransaction t = spyUpdateTransaction();
		addNew(t);
		Table tbl = t.getTable("test");
		int i = 100;
		IndexIter iter = t.iter(tbl.num, null);
		for (iter.next(); ! iter.eof(); iter.next()) {
			int adr = iter.keyadr();
			t.updateRecord(adr, record(i++));
		}
		i = 10;
		iter = t.iter(tbl.num, null);
		for (iter.next(); ! iter.eof(); iter.next()) {
			int adr = iter.keyadr();
			t.updateRecord(adr, record(i++));
		}
		t.ck_complete();
		assertThat(t.counts(), is("+3 -0 u0"));
		rebuild(10, 11, 12);
	}

	@Test
	public void old_multiple_update() {
		makeTable();
		addOld();
		SpyUpdateTransaction t = spyUpdateTransaction();
		Table tbl = t.getTable("test");
		int i = 30;
		IndexIter iter = t.iter(tbl.num, null);
		for (iter.next(); ! iter.eof(); iter.next()) {
			int adr = iter.keyadr();
			t.updateRecord(adr, record(i++));
		}
		i = 20;
		iter = t.iter(tbl.num, null);
		for (iter.next(); ! iter.eof(); iter.next()) {
			int adr = iter.keyadr();
			t.updateRecord(adr, record(i++));
		}
		i = 10;
		iter = t.iter(tbl.num, null);
		for (iter.next(); ! iter.eof(); iter.next()) {
			int adr = iter.keyadr();
			t.updateRecord(adr, record(i++));
		}
		t.ck_complete();
		assertThat(t.counts(), is("+0 -0 u3"));
		rebuild(10, 11, 12);
	}

	@Test
	public void new_update_remove() {
		makeTable();
		SpyUpdateTransaction t = spyUpdateTransaction();
		addNew(t);
		Table tbl = t.getTable("test");
		int i = 100;
		IndexIter iter = t.iter(tbl.num, null);
		for (iter.next(); ! iter.eof(); iter.next()) {
			int adr = iter.keyadr();
			t.updateRecord(adr, record(i++));
		}
		i = 10;
		iter = t.iter(tbl.num, null);
		for (iter.next(); ! iter.eof(); iter.next())
			t.removeRecord(iter.keyadr());
		t.ck_complete();
		assertThat(t.counts(), is("+0 -0 u0"));
		rebuild();
	}

	@Test
	public void old_update_remove() {
		makeTable();
		SpyUpdateTransaction t = spyUpdateTransaction();
		addNew(t);
		t.ck_complete();

		t = spyUpdateTransaction();
		Table tbl = t.getTable("test");
		int i = 100;
		IndexIter iter = t.iter(tbl.num, null);
		for (iter.next(); ! iter.eof(); iter.next()) {
			int adr = iter.keyadr();
			t.updateRecord(adr, record(i++));
		}
		i = 10;
		iter = t.iter(tbl.num, null);
		for (iter.next(); ! iter.eof(); iter.next())
			t.removeRecord(iter.keyadr());
		t.ck_complete();
		assertThat(t.counts(), is("+0 -3 u0"));
		rebuild();
	}

	SpyUpdateTransaction spyUpdateTransaction() {
		return new SpyUpdateTransaction(
				((Database) db).trans.nextNum(false), (Database) db);
	}
	private static class SpyUpdateTransaction extends UpdateTransaction {
		int nAdds, nRemoves, nUpdates;
		SpyUpdateTransaction(int num, Database db) {
			super(num, db);
		}
		@Override
		protected void storeAdd(int act) {
			++nAdds;
			super.storeAdd(act);
		}
		@Override
		protected void storeRemove(int act) {
			++nRemoves;
			super.storeRemove(act);
		}
		@Override
		protected void storeUpdate(int from, int to) {
			++nUpdates;
			--nAdds; // because storeUpdate calls storeAdd
			super.storeUpdate(from, to);
		}
		String counts() {
			return "+" + nAdds + " -" + nRemoves + " u" + nUpdates;
		}
	}

	//--------------------------------------------------------------------------

	protected void rebuild(int... values) {
		check(values);
		Storage dstor = ((Database) db).dstor;
//System.out.println("BEFORE ==================================================");
//((Database) db).dump();
		db.check();
		db.close();
		Rebuild rebuild = new Rebuild(dstor, new HeapStorage());
		rebuild.rebuild();
		db = rebuild.db;
		check(values);
	}

	private static class Rebuild extends DbRebuild {
		Database db;

		Rebuild(Storage dstor, Storage istor) {
			super(dstor, istor);
		}
		@Override
		protected void fix() {
			db = DatabasePackage.dbpkg.testdb();
			try {
				reprocess(db);
//System.out.println("AFTER ===================================================");
//db.dump();
				db.check();
			} finally {
				db.close();
			}
		}
	}

}
