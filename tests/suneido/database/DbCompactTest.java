package suneido.database;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.After;
import org.junit.Test;

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
		return DbCompact.compact(filename);
	}

	@After
	public void remove_bak() {
		new File(filename + ".bak").delete();
	}

}
