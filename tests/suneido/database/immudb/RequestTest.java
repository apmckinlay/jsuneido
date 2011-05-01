/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import org.junit.Test;

import suneido.database.immudb.query.Request;
import suneido.database.immudb.tools.CheckTable;

public class RequestTest {
	private static final String SCHEMA = "(a,b,c) key(a) index(b,c)";
	TestStorage stor = new TestStorage(500, 100);
	Database db = Database.create(stor);

	@Test
	public void create_table() {
		request("create tbl " + SCHEMA);
		assertThat(db.schema().get("tbl").schema(), is(SCHEMA));
	}

	@Test
	public void alter_table() {
		request("create tbl " + SCHEMA);
		db = Database.open(stor);
		assertThat(db.schema().get("tbl").schema(), is(SCHEMA));
		request("alter tbl create (d) index(c,d)");
		assertThat(db.schema().get("tbl").schema(),
				is("(a,b,c,d) key(a) index(b,c) index(c,d)"));
		db = Database.open(stor);
		assertThat(db.schema().get("tbl").schema(),
				is("(a,b,c,d) key(a) index(b,c) index(c,d)"));
	}

	@Test
	public void add_index_to_table_with_data() {
		Request.execute(db, "alter tables create key(tablename,table)");
		assertThat(new CheckTable(db, "tables").call(), is(""));
	}

	@Test
	public void create_when_already_exists() {
		request("create tbl " + SCHEMA);
		try {
			request("create tbl " + SCHEMA);
			fail();
		} catch (RuntimeException e) {
			assertTrue(e.toString().contains("duplicate key"));
		}
	}

	@Test
	public void ensure() {
		request("ensure tbl " + SCHEMA);
		assertThat(db.schema().get("tbl").schema(), is(SCHEMA));
		request("ensure tbl (c, d) index(a) index(c,d)");
		assertThat(db.schema().get("tbl").schema(),
				is("(a,b,c,d) key(a) index(b,c) index(c,d)"));
	}

	private void request(String request) {
		Request.execute(db, request);
	}

}
