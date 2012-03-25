package suneido.immudb;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.intfc.database.DatabasePackage.Status;

public class DbCompactTest extends TestBase {

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
		ImmuDatabase dstdb = (ImmuDatabase) dbpkg().testdb();
		dbpkg().compact(db, dstdb);
		assertEquals(Status.OK, dstdb.check());
	}

}
