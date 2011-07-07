package suneido.database.query;

import static org.junit.Assert.*;

import org.junit.Test;

import suneido.SuException;
import suneido.database.*;
import suneido.database.TestBase;

public class RequestTest extends TestBase {
	@Test
	public void test() {
		String schema = "(a,b,c) key(a)";
		Request.execute(TheDb.db(), "create test " + schema);
		assertEquals(schema, TheDb.db().getTable("test").schema());

		Request.execute(TheDb.db(), "ensure test2 " + schema);
		assertEquals(schema, TheDb.db().getTable("test2").schema());

		Request.execute(TheDb.db(), "ensure test (c,d,e) KEY(a) INDEX(b,c)");
		schema = "(a,b,c,d,e) key(a) index(b,c)";
		assertEquals(schema, TheDb.db().getTable("test").schema());

		String extra = " index(c) in other(cc)";
		Request.execute(TheDb.db(), "alter test create" + extra);
		assertEquals(schema + extra, TheDb.db().getTable("test").schema());

		Request.execute(TheDb.db(), "ALTER test DROP index(c)");
		assertEquals(schema, TheDb.db().getTable("test").schema());

		Request.execute(TheDb.db(), "alter test rename b to bb");
		schema = "(a,bb,c,d,e) key(a) index(bb,c)";
		assertEquals(schema, TheDb.db().getTable("test").schema());

		Request.execute(TheDb.db(), "alter test drop (d,e)");
		schema = "(a,bb,c) key(a) index(bb,c)";
		assertEquals(schema, TheDb.db().getTable("test").schema());

		Request.execute(TheDb.db(), "RENAME test TO tmp");
		assertEquals(schema, TheDb.db().getTable("tmp").schema());
		assertNull(TheDb.db().getTable("test"));

		Request.execute(TheDb.db(), serverData, "drop tmp");
		assertNull(TheDb.db().getTable("tmp"));

		Request.execute(TheDb.db(), "create tmp (aField) key(aField)");

		Request.execute(TheDb.db(), serverData, "drop tmp");
		assertNull(TheDb.db().getTable("tmp"));
	}

	@Test
	public void test_view() {
		String def = "/*comment*/ one join two where three = 4";
		Request.execute(TheDb.db(), serverData, "view myview = " + def);
		Transaction t = TheDb.db().readonlyTran();
		assertEquals(def, t.getView("myview"));
		t.complete();
		Request.execute(TheDb.db(), serverData, "drop myview");
		t = TheDb.db().readonlyTran();
		assertNull(t.getView("myview"));
		t.complete();
	}

	@Test
	public void test_parse_eof() {
		Request.execute(TheDb.db(), "create tmp (aField) key(aField)");
		assertNotNull(TheDb.db().getTable("tmp"));
		try {
			Request.execute(TheDb.db(), serverData, "drop tmp extra");
			fail("should have got an exception");
		} catch (SuException e) {
			assertEquals("syntax error at line 1: expected: EOF got: IDENTIFIER",
					e.toString());
		}
		assertNotNull(TheDb.db().getTable("tmp"));
	}

}
