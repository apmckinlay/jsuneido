/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

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
		Database dstdb = Dbpkg.testdb();
		Dbpkg.compact(db, dstdb);
		assertEquals("", dstdb.check());
	}

}
