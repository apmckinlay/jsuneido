package suneido.database.tools;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.database.Database;
import suneido.database.Mode;

public class DbCompactTest extends DbCheckRebuildBase {

	public DbCompactTest() {
		super("dbcompacttest.db");
	}

	@Test
	public void empty() {
		db = new Database(filename, Mode.CREATE);
		closeDb();
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
			closeDb();
		}
		dbcompact();
		dbcheck();
		checkTable();
	}

	private int dbcompact() {
		return DbCompact.compact(filename, outfilename);
	}

}
