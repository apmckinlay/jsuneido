/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import suneido.database.immudb.query.Request;
import suneido.database.immudb.schema.Bootstrap;

public class RequestTest {

	@Test
	public void test() {
		TestStorage stor = new TestStorage(500, 100);
		new Bootstrap(stor).create();
		Database db = new Database(stor);
		db.open();

		Request.execute(db, "create tbl (a) key (a)");

		db = new Database(stor);
		db.open();
		assertThat(db.schema().get("tbl").schema(),
				is("(a) key(a)"));
	}

}
