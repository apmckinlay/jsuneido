/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SchemaTest extends TestBase {

	@Test
	public void create_index_on_existing_table() {
		final int N = 200;
		makeTable(N);
		String COLS = "a,b";
		db.addIndex("test", COLS, false);
		check(N, COLS);
		reopen();
		check(N, COLS);
	}

	private void check(final int N, String COLS) {
		Transaction t = db.readonlyTran();
		Table tbl = t.getTable("test");
		try {
			BtreeIndex bi = t.getBtreeIndex(tbl.num, COLS);
			BtreeIndex.Iter iter = bi.iter(t);
			for (int i = 0; i < N; ++i) {
				iter.next();
				assert ! iter.eof() : "premature eof at " + i;
			}
			iter.next();
			assertTrue(iter.eof());
		} finally {
			t.complete();
		}
	}

}
