/* Copyright 2009 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;
import static suneido.util.testing.Throwing.assertThrew;

import org.junit.Test;

import suneido.SuException;
import suneido.intfc.database.Transaction;

public class RequestTest extends TestBase {

	@Test
	public void test() {
		String schema = "(a,b,c) key(a)";
		Request.execute(db, "create test1 " + schema);
		assertEquals(schema, db.getSchema("test1"));

		Request.execute(db, "ensure test2 " + schema);
		assertEquals(schema, db.getSchema("test2"));

		Request.execute(db, "ensure test1 (c,d,e) KEY(a) INDEX(b,c)");
		schema = "(a,b,c,d,e) key(a) index(b,c)";
		assertEquals(schema, db.getSchema("test1"));

		String extra = " index(c) in tables(table)";
		Request.execute(db, "alter test1 create" + extra);
		assertEquals(schema + extra, db.getSchema("test1"));

		Request.execute(db, "ALTER test1 DROP index(c)");
		assertEquals(schema, db.getSchema("test1"));

		Request.execute(db, "alter test1 rename b to bb");
		schema = "(a,bb,c,d,e) key(a) index(bb,c)";
		assertEquals(schema, db.getSchema("test1"));

		Request.execute(db, "alter test1 drop (d,e)");
		schema = "(a,bb,c) key(a) index(bb,c)";
		assertEquals(schema, db.getSchema("test1"));

		Request.execute(db, "RENAME test1 TO tmp");
		assertEquals(schema, db.getSchema("tmp"));
		assertNull(db.getSchema("test1"));

		Request.execute(db, serverData, "drop tmp");
		assertNull(db.getSchema("tmp"));

		Request.execute(db, "create tmp (aField) key(aField)");

		Request.execute(db, serverData, "drop tmp");
		assertNull(db.getSchema("tmp"));
	}

	@Test
	public void test_ensure() {
		Request.execute(db, "create xxx (a,b,c) key(a) index(b)");
		Request.execute(db, "ensure xxx (b,c,d) index(b) index(c) index(d)");
		assertThat(db.getSchema("xxx"),
				equalTo("(a,b,c,d) key(a) index(b) index(c) index(d)"));
	}

	@Test
	public void test_existing() {
		Request.execute(db, "create xxx (a,b,c) key(a) index(b)");
		shouldFail("alter xxx create (b)", "column already exists");
		shouldFail("alter xxx create index(b)", "index already exists");

		Request.execute(db, "create yyy (k) key(k)");
		shouldFail("rename yyy to xxx", "exist");
	}

	private void shouldFail(String request, String expected) {
		try {
			Request.execute(db, request);
			fail();
		} catch (RuntimeException e) {
			assertThat(e.toString(), containsString(expected));
		}
	}

	@Test
	public void test_view() {
		String def = "/*comment*/ one join two where three = 4";
		Request.execute(db, serverData, "view myview = " + def);
		Transaction t = db.readTransaction();
		assertEquals(def, t.getView("myview"));
		t.complete();
		Request.execute(db, serverData, "drop myview");
		t = db.readTransaction();
		assertEquals(null, t.getView("myview"));
		t.complete();
	}

	@Test
	public void test_parse_eof() {
		Request.execute(db, "create tmp (aField) key(aField)");
		assertNotNull(db.getSchema("tmp"));
		assertThrew(() -> {
			Request.execute(db, serverData, "drop tmp extra");
		}, SuException.class,
				"syntax error at line 1.*: expected: EOF got: IDENTIFIER");
		assertNotNull(db.getSchema("tmp"));
	}

}
