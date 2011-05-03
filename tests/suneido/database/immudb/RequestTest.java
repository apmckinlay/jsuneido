/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static suneido.database.immudb.TableBuilder.*;

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
	public void alter_table_create() {
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

	@Test
	public void drop_columns() {
		request("create tbl (a,b,c,d) key(a)");
		request("alter tbl drop (b,d)");
		assertThat(db.schema().get("tbl").schema(), is("(a,c) key(a)"));
		db = Database.open(stor);
		assertThat(db.schema().get("tbl").schema(), is("(a,c) key(a)"));
	}

	@Test
	public void drop_nonexistent_column() {
		request("ensure tbl " + SCHEMA);
		try {
			request("alter tbl drop (x)");
			fail();
		} catch (Exception e) {
			assertThat(e.getMessage(), containsString(NONEXISTENT_COLUMN));
		}
	}

	@Test
	public void drop_column_used_in_index() {
		request("ensure tbl " + SCHEMA);
		try {
			request("alter tbl drop (b)");
			fail();
		} catch (Exception e) {
			assertThat(e.getMessage(), containsString(CANT_DROP_COLUMN_IN_INDEX));
		}
	}

	@Test
	public void drop_index() {
		request("ensure tbl " + SCHEMA);
		request("alter tbl drop index(b,c)");
		assertThat(db.schema().get("tbl").schema(), is("(a,b,c) key(a)"));
		db = Database.open(stor);
		assertThat(db.schema().get("tbl").schema(), is("(a,b,c) key(a)"));
	}

	@Test
	public void drop_nonexistent_index() {
		request("ensure tbl " + SCHEMA);
		try {
			request("alter tbl drop index(x)");
			fail();
		} catch (Exception e) {
			assertThat(e.getMessage(), containsString(NONEXISTENT_COLUMN));
		}
		try {
			request("alter tbl drop index(c,b,a)");
			fail();
		} catch (Exception e) {
			assertThat(e.getMessage(), containsString(NONEXISTENT_INDEX));
		}
	}

	@Test
	public void cant_create_table_without_key() {
		try {
			request("create tbl (a,b,c) index(a)");
		} catch (Exception e) {
			assertThat(e.getMessage(), containsString(TABLE_MUST_HAVE_KEY));
		}
	}

	@Test
	public void cant_remove_last_key() {
		request("ensure tbl " + SCHEMA);
		try {
			request("alter tbl drop key(a)");
		} catch (Exception e) {
			assertThat(e.getMessage(), containsString(TABLE_MUST_HAVE_KEY));
		}
	}

	@Test
	public void drop_table() {
		request("ensure tbl " + SCHEMA);
		request("drop tbl");
		assertNull(db.schema().get("tbl"));
		db = Database.open(stor);
		assertNull(db.schema().get("tbl"));
	}

	@Test
	public void drop_nonexistent_table() {
		try {
			request("drop tbl");
			fail();
		} catch (Exception e) {
			assertThat(e.getMessage(), containsString(NONEXISTENT_TABLE));
		}
	}

	private void request(String request) {
		Request.execute(db, request);
	}

}
