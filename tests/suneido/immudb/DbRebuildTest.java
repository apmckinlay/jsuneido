/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import org.junit.Test;

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

	protected void rebuild(int... values) {
		check(values);
		Storage dstor = ((Database) db).dstor;
//System.out.println("BEFORE ==================================================");
//((Database) db).dump();
		db.check();
		db.close();
		Rebuild rebuild = new Rebuild(dstor, new MemStorage());
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
