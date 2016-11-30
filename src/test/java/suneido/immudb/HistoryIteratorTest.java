/* Copyright 2016 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.intfc.database.Record;
import suneido.intfc.database.Transaction;

public class HistoryIteratorTest extends TestBase {

	@Test
	public void test_empty() {
		makeTable("one", 5);
		makeTable(0);
		makeTable("two", 5);
		history();
	}

	@Test
	public void test_history() {
		makeTable("one", 5);
		makeTable(2);
		makeTable("two", 5);
		remove(1);
		addRecords("test", 2, 3);
		update(2, rec(22));
		history("create 0", "create 1", "delete 1", "create 2", "create 3",
				"delete 2", "create 22");
	}

	@Test
	public void test_schema() {
		history("tables", 1, "create tables", "create columns", "create indexes",
				"create views");
	}

	private void history(String... expected) {
		history("test", 0, expected);
	}

	private void history(String tablename, int fld, String... expected) {
		Transaction t = db.readTransaction();
		int tblnum = t.getTable(tablename).num();
		// forward
		HistoryIterator iter = new HistoryIterator(((Database) db).dstor, tblnum);
		for (String expect : expected) {
			Record[] x = iter.getNext();
			//System.out.println(x[0].get(1) + " " + x[1].get(fld));
			assertEquals(expect, x[0].get(1) + " " + x[1].get(fld));
		}
		assert iter.getNext() == null;

		// reverse
		iter = new HistoryIterator(((Database) db).dstor, tblnum);
		for (int i = expected.length - 1; i >= 0; --i) {
			String expect = expected[i];
			Record[] x = iter.getPrev();
			assertEquals(expect, x[0].get(1) + " " + x[1].get(fld));
		}
		assert iter.getPrev() == null;
	}

}
