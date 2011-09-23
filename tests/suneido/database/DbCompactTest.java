package suneido.database;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DbCompactTest extends DbCheckRebuildTestBase {

	@Test
	public void empty() {
		Database.create(filename).close();
		int n = dbcompact();
		assertEquals(1, n);
		dbcheckout();
	}

	@Test
	public void simple() {
		db = Database.create(filename);
		try {
			makeTable("mytable", 4);
		} finally {
			db.close();
		}
		dbcompact();
		dbcheckout();
		checkTable();
	}

	private int dbcompact() {
		db = Database.openReadonly(filename);
		try {
			Database outdb = Database.create(outfilename);
			try {
				return DbCompact.compact(db, outdb);
			} finally {
				outdb.close();
			}
		} finally {
			db.close();
		}
	}

}
