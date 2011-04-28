/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import suneido.database.immudb.query.Request;

public class RequestTest {
	private static final String SCHEMA = "(a,b,c) key(a) index(b,c)";

	@Test
	public void test() {
		TestStorage stor = new TestStorage(500, 100);
		Database db = Database.create(stor);

		db = Database.open(stor);
		String schema = SCHEMA;
		Request.execute(db, "create tbl " + schema);
		check(db);

		db = Database.open(stor);
		check(db);
	}

	private void check(Database db) {
		assertThat(db.schema().get("tables").schema(),
				is("(table,tablename) key(table) key(tablename)"));
		assertThat(db.schema().get("tbl").schema(), is(SCHEMA));
	}

}
