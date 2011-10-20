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

import suneido.database.query.*;
import suneido.database.query.Query.Dir;
import suneido.database.server.ServerData;
import suneido.intfc.database.DatabasePackage.Status;
import suneido.intfc.database.Transaction;

public class RequestTest {
	private static final String SCHEMA = "(a,b,c) key(a) index(b,c)";
	private static final ServerData serverData = new ServerData();
	MemStorage stor = new MemStorage(1000, 100);
	Database db = Database.create(stor);

	@Test
	public void create_table() {
		req("create tbl " + SCHEMA);
		assertThat(db.getSchema("tbl"), is(SCHEMA));
	}

	@Test
	public void create_when_already_exists() {
		req("create tbl " + SCHEMA);
		try {
			req("create tbl " + SCHEMA);
			fail();
		} catch (RuntimeException e) {
			assertThat(e.toString(), containsString("existing table"));
		}
	}

	@Test
	public void alter_table_create() {
		req("create tbl " + SCHEMA);
		db = db.reopen();
		assertThat(db.getSchema("tbl"), is(SCHEMA));
		req("alter tbl create (d) index(c,d)");
		assertThat(db.getSchema("tbl"),
				is("(a,b,c,d) key(a) index(b,c) index(c,d)"));
		db = db.reopen();
		assertThat(db.getSchema("tbl"),
				is("(a,b,c,d) key(a) index(b,c) index(c,d)"));
	}

	@Test
	public void add_index_to_table_with_data() {
		req("create tbl " + SCHEMA);
		req("alter tbl create index(c,a)");
	}

	@Test
	public void create_with_rule_fields() {
		String schema = "(a,b,C,D) key(a)";
		req("create tbl " + schema);
		assertThat(db.getSchema("tbl"), is(schema));
	}

	@Test
	public void empty_key() {
		String schema = "(a,b,c) key()";
		req("create tbl " + schema);
		assertThat(db.getSchema("tbl"), is(schema));
	}

	@Test
	public void no_columns() {
		String schema = "() key()";
		req("create tbl " + schema);
		assertThat(db.getSchema("tbl"), is(schema));
		db = db.reopen();
		assertThat(db.getSchema("tbl"), is(schema));
	}

	@Test
	public void ensure() {
		req("ensure tbl " + SCHEMA);
		assertThat(db.getSchema("tbl"), is(SCHEMA));
		req("ensure tbl (c, d) index(a) index(c,d)");
		assertThat(db.getSchema("tbl"),
				is("(a,b,c,d) key(a) index(b,c) index(c,d)"));
	}

	@Test
	public void drop_columns() {
		req("create tbl (a,b,c,d) key(a)");
		req("alter tbl drop (b,d)");
		assertThat(db.getSchema("tbl"), is("(a,c) key(a)"));
		db = db.reopen();
		assertThat(db.getSchema("tbl"), is("(a,c) key(a)"));
	}

	@Test
	public void drop_nonexistent_column() {
		req("ensure tbl " + SCHEMA);
		try {
			req("alter tbl drop (x)");
			fail();
		} catch (Exception e) {
			assertThat(e.getMessage(), containsString("nonexistent column"));
		}
	}

	@Test
	public void drop_column_used_in_index() {
		req("ensure tbl " + SCHEMA);
		try {
			req("alter tbl drop (b)");
			fail();
		} catch (Exception e) {
			assertThat(e.getMessage(), containsString("column used in index"));
		}
	}

	@Test
	public void drop_index() {
		req("ensure tbl " + SCHEMA);
		req("alter tbl drop index(b,c)");
		assertThat(db.getSchema("tbl"), is("(a,b,c) key(a)"));
		db = db.reopen();
		assertThat(db.getSchema("tbl"), is("(a,b,c) key(a)"));
	}

	@Test
	public void drop_nonexistent_index() {
		req("ensure tbl " + SCHEMA);
		try {
			req("alter tbl drop index(x)");
			fail();
		} catch (Exception e) {
			assertThat(e.getMessage(), containsString("nonexistent column"));
		}
		try {
			req("alter tbl drop index(c,b,a)");
			fail();
		} catch (Exception e) {
			assertThat(e.getMessage(), containsString("nonexistent index"));
		}
	}

	@Test
	public void cant_create_table_without_key() {
		try {
			req("create tbl (a,b,c) index(a)");
		} catch (Exception e) {
			assertThat(e.getMessage(), containsString("key required"));
		}
	}

	@Test
	public void cant_remove_last_key() {
		req("ensure tbl " + SCHEMA);
		try {
			req("alter tbl drop key(a)");
		} catch (Exception e) {
			assertThat(e.getMessage(), containsString("key required"));
		}
	}

	@Test
	public void drop_table() {
		req("ensure tbl " + SCHEMA);
		req("drop tbl");
		assertNull(db.schema().get("tbl"));
		db = db.reopen();
		assertNull(db.schema().get("tbl"));
	}

	@Test
	public void drop_nonexistent_table() {
		try {
			req("drop tbl");
			fail();
		} catch (Exception e) {
			assertThat(e.getMessage(), containsString("nonexistent table"));
		}
	}

	@Test
	public void rename_table() {
		req("ensure tbl " + SCHEMA);
		req("rename tbl to lbt");
		assertNull(db.getSchema("tbl"));
		assertThat(db.getSchema("lbt"), is(SCHEMA));
		db = db.reopen();
		assertNull(db.getSchema("tbl"));
		assertThat(db.getSchema("lbt"), is(SCHEMA));
	}

	@Test
	public void rename_nonexistent_table() {
		try {
			req("rename tbl to lbt");
			fail();
		} catch (Exception e) {
			assertThat(e.getMessage(), containsString("nonexistent table"));
		}
	}

	@Test
	public void next_column_num() {
		req("ensure tbl (a,b,c) key(a)");
		req("alter tbl drop (b)");
		assertThat(db.getSchema("tbl"), is("(a,c) key(a)"));
		req("alter tbl create (d) index(d)");
		assertThat(db.getSchema("tbl"), is("(a,c,d) key(a) index(d)"));
	}

	@Test
	public void rename_columns() {
		req("ensure tbl " + SCHEMA);
		req("alter tbl rename b to bb, c to cc");
		assertThat(db.getSchema("tbl"), is("(a,bb,cc) key(a) index(bb,cc)"));
		db = db.reopen();
		assertThat(db.getSchema("tbl"), is("(a,bb,cc) key(a) index(bb,cc)"));
	}

	@Test
	public void creates() {
		req("create Accountinglib (name,text,num,parent,group,lib_committed,lib_modified) key(name,group) key(num) index(parent,name)");
		req("create Contrib (num,parent,group,name,text,lib_committed,lib_modified) key(name,group) key(num) index(parent,name)");
		req("create ETA (path,name,order,text,num,lib_committed,lib_modified,plugin) index(name) key(num) index(order,name) key(path,name) index(path,order,name) index(plugin)");
		req("create ETAHelp (path,name,order,text,num,lib_committed,lib_modified,plugin) index(name) key(num) index(order,name) key(path,name) index(path,order,name) index(plugin)");
	}

	@Test
	public void ensure_readonly() {
		req("ensure tbl " + SCHEMA);
		ExclusiveTransaction t = db.exclusiveTran();
		req("ensure tbl " + SCHEMA);
		t.abort();
	}

	@Test
	public void add_remove_fields() {
		req("create tbl (a, b, c, d, e, f, g) key(b)");
		assertThat(db.getSchema("tbl"), is("(a,b,c,d,e,f,g) key(b)"));
		exec("insert { a: 1, b: 2, c: 3, d: 4, e: 5, f: 6, g: 7 } into tbl");
		req("alter tbl drop (a, c, e, g)");
		assertThat(db.getSchema("tbl"), is("(b,d,f) key(b)"));
		req("alter tbl create (h, i, j, k)");
		assertThat(db.getSchema("tbl"), is("(b,d,f,h,i,j,k) key(b)"));
		assertThat(first(), is("Row{b: 2, d: 4, f: 6}"));
	}

	private String first() {
		Transaction t = db.readonlyTran();
		Query q = CompileQuery.query(t, serverData, "tbl");
		Header hdr = q.header();
		Row row = q.get(Dir.NEXT);
		t.ck_complete();
		return row == null ? null : row.toString(hdr);
	}

	@After
	public void check() {
		assertThat(DbCheck.check(stor), is(Status.OK));
	}

	private void req(String request) {
		Request.execute(db, request);
	}

	protected int exec(String s) {
		Transaction t = db.readwriteTran();
		try {
			Query q = CompileQuery.parse(t, serverData, s);
			int n = ((QueryAction) q).execute();
			t.ck_complete();
			return n;
		} finally {
			t.abortIfNotComplete();
		}
	}

}
