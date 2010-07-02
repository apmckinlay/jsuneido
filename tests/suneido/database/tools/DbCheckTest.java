package suneido.database.tools;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.database.Database;
import suneido.database.Mode;
import suneido.database.tools.DbCheck.Status;

public class DbCheckTest extends DbCheckRebuildBase {

	@Test
	public void test_empty() {
		db = new Database(filename, Mode.CREATE);
		db.close();
		db = null;
		dbcheck();
	}

	@Test
	public void test_simple() {
		db = new Database(filename, Mode.CREATE);
		try {
			makeTable("mytable", 4);
		} finally {
			closeDb();
		}
		dbcheck();
		checkTable();
	}

	@Test
	public void test_rename_table() {
		db = new Database(filename, Mode.CREATE);
		try {
			makeTable("tmp", 4);
			db.removeTable("tmp"); // so new db has different offsets
			makeTable("mytable_before", 4);
			db.renameTable("mytable_before", "mytable");
			addRecords("mytable", 4, 7);
		} finally {
			closeDb();
		}
		dbcheck();
		checkTable(8);
	}

	@Override
        protected void dbcheck() {
		assertEquals(Status.OK, DbCheck.check(filename));
	}

}
