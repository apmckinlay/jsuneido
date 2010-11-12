package suneido.database.tools;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.database.Mode;
import suneido.database.TheDb;

public class DbCompactTest extends DbCheckRebuildBase {

	@Test
	public void empty() {
		TheDb.open(filename, Mode.CREATE);
		TheDb.close();
		int n = dbcompact();
		assertEquals(1, n);
		dbcheck();
	}

	@Test
	public void simple() {
		TheDb.open(filename, Mode.CREATE);
		try {
			makeTable("mytable", 4);
		} finally {
			TheDb.close();
		}
		dbcompact();
		dbcheck();
		checkTable();
	}

	private int dbcompact() {
		return DbCompact.compact(filename, outfilename);
	}

}
