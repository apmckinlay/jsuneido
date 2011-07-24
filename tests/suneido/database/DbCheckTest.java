package suneido.database;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.database.DbCheck.Status;

public class DbCheckTest extends DbCheckRebuildTestBase {

	@Test
	public void test_empty() {
		new Database(filename, Mode.CREATE).close();
		dbcheck();
	}

	@Test
	public void test_simple() {
		db = new Database(filename, Mode.CREATE);
		try {
			makeTable("mytable", 4);
		} finally {
			db.close();
		}
		dbcheck();
		checkTable();
	}

	@Test
	public void test_rename_table() {
		db = new Database(filename, Mode.CREATE);
		try {
			makeTable("tmp", 4);
			db.removeTable("tmp"); // so new theDB() has different offsets
			makeTable("mytable_before", 4);
			db.renameTable("mytable_before", "mytable");
			addRecords("mytable", 4, 7);
		} finally {
			db.close();
		}
		dbcheck();
		checkTable(8);
	}

	@Override
        protected void dbcheck() {
		assertEquals(Status.OK, DbCheck.check(filename));
	}

}
