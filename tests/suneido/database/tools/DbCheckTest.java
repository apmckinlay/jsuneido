package suneido.database.tools;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.database.Mode;
import suneido.database.TheDb;
import suneido.database.tools.DbCheck.Status;

public class DbCheckTest extends DbCheckRebuildBase {

	@Test
	public void test_empty() {
		TheDb.open(filename, Mode.CREATE);
		TheDb.close();
		dbcheck();
	}

	@Test
	public void test_simple() {
		TheDb.open(filename, Mode.CREATE);
		try {
			makeTable("mytable", 4);
		} finally {
			TheDb.close();
		}
		dbcheck();
		checkTable();
	}

	@Test
	public void test_rename_table() {
		TheDb.open(filename, Mode.CREATE);
		try {
			makeTable("tmp", 4);
			TheDb.db().removeTable("tmp"); // so new theDB() has different offsets
			makeTable("mytable_before", 4);
			TheDb.db().renameTable("mytable_before", "mytable");
			addRecords("mytable", 4, 7);
		} finally {
			TheDb.close();
		}
		dbcheck();
		checkTable(8);
	}

	@Override
        protected void dbcheck() {
		assertEquals(Status.OK, DbCheck.check(filename));
	}

}
