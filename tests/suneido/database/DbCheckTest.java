package suneido.database;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.intfc.database.DatabasePackage.Status;

public class DbCheckTest extends DbCheckRebuildTestBase {

	@Test
	public void test_empty() {
		Database.create(filename).close();
		dbcheck();
	}

	@Test
	public void test_simple() {
		db = Database.create(filename);
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
		db = Database.create(filename);
		try {
			makeTable("tmp", 4);
			db.dropTable("tmp"); // so new theDB() has different offsets
			makeTable("mytable_before", 4);
			db.renameTable("mytable_before", "mytable");
			addRecords("mytable", 4, 7);
		} finally {
			db.close();
		}
		dbcheck();
		checkTable(8);
	}

	private void dbcheck() {
		assertEquals(Status.OK, DbCheck.check(filename));
	}

}
