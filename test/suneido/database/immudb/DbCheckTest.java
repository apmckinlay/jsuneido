/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DbCheckTest extends TestBase {

	@Test
	public void test_empty() {
		dbcheck();
	}

	@Test
	public void test_simple() {
		makeTable(99);

		ReadTransaction t = readTransaction();
		t.getIndex(1, 0).check();
		t.getIndex(1, 1).check();
		t.getIndex(2, 0,2).check();
		t.getIndex(3, 0,1).check();
		t.getIndex(4, 0).check();
		t.getIndex(5, 0).check();
		t.getIndex(5, 1,0).check();
		t.ck_complete();

		((Database) db).persist();

		t = readTransaction();
		t.getIndex(1, 0).check();
		t.getIndex(1, 1).check();
		t.getIndex(2, 0,2).check();
		t.getIndex(3, 0,1).check();
		t.getIndex(4, 0).check();
		t.getIndex(5, 0).check();
		t.getIndex(5, 1,0).check();
		t.ck_complete();

		dbcheck();
	}

	protected void dbcheck() {
		assertEquals("", db.check());
	}

}
