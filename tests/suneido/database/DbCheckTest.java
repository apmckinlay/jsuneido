package suneido.database;

import org.junit.Test;

public class DbCheckTest extends DbCheckRebuildBase {

	public DbCheckTest() {
		super("dbchecktest.db");
	}

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

}
