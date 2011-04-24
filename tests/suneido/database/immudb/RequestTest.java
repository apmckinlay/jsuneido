/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import suneido.database.immudb.query.Request;

public class RequestTest {

	@Test
	public void test() {
		TestStorage stor = new TestStorage(500, 100);
		Database db = new Database(stor);
		db.create();

		db = new Database(stor);
		db.open();
		Request.execute(db, "create tbl (a) key (a)");

		db = new Database(stor);
		db.open();
		assertThat(db.schema().get("tables").schema(),
				is("(table,tablename) key(table)"));
		assertThat(db.schema().get("tbl").schema(),
				is("(a) key(a)"));
	}

}
