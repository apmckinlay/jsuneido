/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.intfc.database.DatabasePackage.Status;

public class DbCheckTest extends TestBase {

	@Test
	public void test_empty() {
		dbcheck();
	}

	@Test
	public void test_simple() {
		makeTable(99);
		dbcheck();
	}

	protected void dbcheck() {
		assertEquals(Status.OK, db.check());
	}

}
