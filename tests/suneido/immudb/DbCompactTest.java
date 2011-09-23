package suneido.immudb;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.intfc.database.DatabasePackage.Status;
import suneido.intfc.database.TestBase;

public class DbCompactTest extends TestBase {

	@Override
	protected DatabasePackage dbpkg() {
		return DatabasePackage.dbpkg;
	}

	@Test
	public void empty() {
		compact();
	}

	@Test
	public void simple() {
		makeTable(99);
		compact();
	}

	private void compact() {
		Database dstdb = dbpkg().testdb();
		dbpkg().compact(db, dstdb);
		assertEquals(Status.OK, dstdb.check());
	}

}
