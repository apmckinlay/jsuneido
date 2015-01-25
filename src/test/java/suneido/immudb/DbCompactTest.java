/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.intfc.database.Database;

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
		Database dstdb = dbpkg().testdb();
		dbpkg().compact(db, dstdb);
		assertEquals("", dstdb.check());
	}

}
