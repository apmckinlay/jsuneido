/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Test;

import suneido.database.query.Request;
import suneido.intfc.database.DatabasePackage.Status;

public class RequestTest {
	private static final String SCHEMA = "(a,b,c) key(a) index(b,c)";
	MemStorage stor = new MemStorage(1000, 100);
	Database db = Database.create(stor);

	@Test
	public void create_table() {
		request("create tbl " + SCHEMA);
		assertThat(db.getSchema("tbl"), is(SCHEMA));
	}

	@Test
	public void create_when_already_exists() {
		request("create tbl " + SCHEMA);
		try {
			request("create tbl " + SCHEMA);
			fail();
		} catch (RuntimeException e) {
			assertThat(e.toString(), containsString("existing table"));
		}
	}

	@Test
	public void alter_table_create() {
		request("create tbl " + SCHEMA);
		db = db.reopen();
		assertThat(db.getSchema("tbl"), is(SCHEMA));
		request("alter tbl create (d) index(c,d)");
		assertThat(db.getSchema("tbl"),
				is("(a,b,c,d) key(a) index(b,c) index(c,d)"));
		db = db.reopen();
		assertThat(db.getSchema("tbl"),
				is("(a,b,c,d) key(a) index(b,c) index(c,d)"));
	}

	@Test
	public void add_index_to_table_with_data() {
		request("create tbl " + SCHEMA);
		request("alter tbl create index(c,a)");
	}

	@Test
	public void create_with_rule_fields() {
		String schema = "(a,b,C,D) key(a)";
		request("create tbl " + schema);
		assertThat(db.getSchema("tbl"), is(schema));
	}

	@Test
	public void empty_key() {
		String schema = "(a,b,c) key()";
		request("create tbl " + schema);
		assertThat(db.getSchema("tbl"), is(schema));
	}

	@Test
	public void no_columns() {
		String schema = "() key()";
		request("create tbl " + schema);
		assertThat(db.getSchema("tbl"), is(schema));
		db = db.reopen();
		assertThat(db.getSchema("tbl"), is(schema));
	}

	@Test
	public void ensure() {
		request("ensure tbl " + SCHEMA);
		assertThat(db.getSchema("tbl"), is(SCHEMA));
		request("ensure tbl (c, d) index(a) index(c,d)");
		assertThat(db.getSchema("tbl"),
				is("(a,b,c,d) key(a) index(b,c) index(c,d)"));
	}

	@Test
	public void drop_columns() {
		request("create tbl (a,b,c,d) key(a)");
		request("alter tbl drop (b,d)");
		assertThat(db.getSchema("tbl"), is("(a,c) key(a)"));
		db = db.reopen();
		assertThat(db.getSchema("tbl"), is("(a,c) key(a)"));
	}

	@Test
	public void drop_nonexistent_column() {
		request("ensure tbl " + SCHEMA);
		try {
			request("alter tbl drop (x)");
			fail();
		} catch (Exception e) {
			assertThat(e.getMessage(), containsString("nonexistent column"));
		}
	}

	@Test
	public void drop_column_used_in_index() {
		request("ensure tbl " + SCHEMA);
		try {
			request("alter tbl drop (b)");
			fail();
		} catch (Exception e) {
			assertThat(e.getMessage(), containsString("column used in index"));
		}
	}

	@Test
	public void drop_index() {
		request("ensure tbl " + SCHEMA);
		request("alter tbl drop index(b,c)");
		assertThat(db.getSchema("tbl"), is("(a,b,c) key(a)"));
		db = db.reopen();
		assertThat(db.getSchema("tbl"), is("(a,b,c) key(a)"));
	}

	@Test
	public void drop_nonexistent_index() {
		request("ensure tbl " + SCHEMA);
		try {
			request("alter tbl drop index(x)");
			fail();
		} catch (Exception e) {
			assertThat(e.getMessage(), containsString("nonexistent column"));
		}
		try {
			request("alter tbl drop index(c,b,a)");
			fail();
		} catch (Exception e) {
			assertThat(e.getMessage(), containsString("nonexistent index"));
		}
	}

	@Test
	public void cant_create_table_without_key() {
		try {
			request("create tbl (a,b,c) index(a)");
		} catch (Exception e) {
			assertThat(e.getMessage(), containsString("key required"));
		}
	}

	@Test
	public void cant_remove_last_key() {
		request("ensure tbl " + SCHEMA);
		try {
			request("alter tbl drop key(a)");
		} catch (Exception e) {
			assertThat(e.getMessage(), containsString("key required"));
		}
	}

	@Test
	public void drop_table() {
		request("ensure tbl " + SCHEMA);
		request("drop tbl");
		assertNull(db.schema().get("tbl"));
		db = db.reopen();
		assertNull(db.schema().get("tbl"));
	}

	@Test
	public void drop_nonexistent_table() {
		try {
			request("drop tbl");
			fail();
		} catch (Exception e) {
			assertThat(e.getMessage(), containsString("nonexistent table"));
		}
	}

	@Test
	public void rename_table() {
		request("ensure tbl " + SCHEMA);
		request("rename tbl to lbt");
		assertNull(db.getSchema("tbl"));
		assertThat(db.getSchema("lbt"), is(SCHEMA));
		db = db.reopen();
		assertNull(db.getSchema("tbl"));
		assertThat(db.getSchema("lbt"), is(SCHEMA));
	}

	@Test
	public void rename_nonexistent_table() {
		try {
			request("rename tbl to lbt");
			fail();
		} catch (Exception e) {
			assertThat(e.getMessage(), containsString("nonexistent table"));
		}
	}

	@Test
	public void next_column_num() {
		request("ensure tbl (a,b,c) key(a)");
		request("alter tbl drop (b)");
		assertThat(db.getSchema("tbl"), is("(a,c) key(a)"));
		request("alter tbl create (d) index(d)");
		assertThat(db.getSchema("tbl"), is("(a,c,d) key(a) index(d)"));
	}

	@Test
	public void rename_columns() {
		request("ensure tbl " + SCHEMA);
		request("alter tbl rename b to bb, c to cc");
		assertThat(db.getSchema("tbl"), is("(a,bb,cc) key(a) index(bb,cc)"));
		db = db.reopen();
		assertThat(db.getSchema("tbl"), is("(a,bb,cc) key(a) index(bb,cc)"));
	}

	@Test
	public void creates() {
		request("create Accountinglib (name,text,num,parent,group,lib_committed,lib_modified) key(name,group) key(num) index(parent,name)");
		request("create Contrib (num,parent,group,name,text,lib_committed,lib_modified) key(name,group) key(num) index(parent,name)");
		request("create ETA (path,name,order,text,num,lib_committed,lib_modified,plugin) index(name) key(num) index(order,name) key(path,name) index(path,order,name) index(plugin)");
		request("create ETAHelp (path,name,order,text,num,lib_committed,lib_modified,plugin) index(name) key(num) index(order,name) key(path,name) index(path,order,name) index(plugin)");
	}

	@After
	public void check() {
		assertThat(DbCheck.check(stor), is(Status.OK));
	}

	private void request(String request) {
		Request.execute(db, request);
	}

}
