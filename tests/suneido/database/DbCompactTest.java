package suneido.database;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.database.*;

public class DbCompactTest extends DbCheckRebuildTestBase {

	@Test
	public void empty() {
		new Database(filename, Mode.CREATE).close();
		int n = dbcompact();
		assertEquals(1, n);
		dbcheck();
	}

	@Test
	public void simple() {
		db = new Database(filename, Mode.CREATE);
		try {
			makeTable("mytable", 4);
		} finally {
			db.close();
		}
		dbcompact();
		dbcheck();
		checkTable();
	}

	private int dbcompact() {
		return DbCompact.compact(filename, outfilename);
	}

}
