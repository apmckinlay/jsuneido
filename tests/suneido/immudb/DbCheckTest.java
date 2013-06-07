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

		ReadTransaction t = readTransaction();
		t.getIndex(1, new int[] { 0 }).check();
		t.getIndex(1, new int[] { 1 }).check();
		t.getIndex(2, new int[] { 0,2 }).check();
		t.getIndex(3, new int[] { 0,1 }).check();
		t.getIndex(4, new int[] { 0 }).check();
		t.getIndex(5, new int[] { 0 }).check();
		t.getIndex(5, new int[] { 1,0 }).check();
		t.ck_complete();

		((Database) db).persist();

		t = readTransaction();
		t.getIndex(1, new int[] { 0 }).check();
		t.getIndex(1, new int[] { 1 }).check();
		t.getIndex(2, new int[] { 0,2 }).check();
		t.getIndex(3, new int[] { 0,1 }).check();
		t.getIndex(4, new int[] { 0 }).check();
		t.getIndex(5, new int[] { 0 }).check();
		t.getIndex(5, new int[] { 1,0 }).check();
		t.ck_complete();

		dbcheck();
	}

	protected void dbcheck() {
		assertEquals(Status.OK, db.check());
	}

}