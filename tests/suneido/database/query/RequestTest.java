package suneido.database.query;

import static org.junit.Assert.*;

import org.junit.Test;

import suneido.SuException;
import suneido.database.TestBase;

public class RequestTest extends TestBase {
	@Test
	public void test() {
		String schema = "(a,b,c) key(a)";
		Request.execute("create test " + schema);
		assertEquals(schema, db.schema("test"));

		Request.execute("ensure test2 " + schema);
		assertEquals(schema, db.schema("test2"));

		Request.execute("ensure test (c,d,e) KEY(a) INDEX(b,c)");
		schema = "(a,b,c,d,e) key(a) index(b,c)";
		assertEquals(schema, db.schema("test"));

		String extra = " index(c) in other(cc)";
		Request.execute("alter test create" + extra);
		assertEquals(schema + extra, db.schema("test"));

		Request.execute("ALTER test DROP index(c)");
		assertEquals(schema, db.schema("test"));

		Request.execute("alter test rename b to bb");
		schema = "(a,bb,c,d,e) key(a) index(bb,c)";
		assertEquals(schema, db.schema("test"));

		Request.execute("alter test drop (d,e)");
		schema = "(a,bb,c) key(a) index(bb,c)";
		assertEquals(schema, db.schema("test"));

		Request.execute("RENAME test TO tmp");
		assertEquals(schema, db.schema("tmp"));
		assertNull(db.tables.get("test"));

		Request.execute(serverData, "drop tmp");
		assertNull(db.tables.get("tmp"));

		Request.execute("create tmp (aField) key(aField)");

		Request.execute(serverData, "drop tmp");
		assertNull(db.tables.get("tmp"));
	}

	@Test
	public void test_view() {
		String def = "one join two where three = 4";
		Request.execute(serverData, "view myview = " + def);
		assertEquals(def, db.cursorTran().getView("myview"));
		Request.execute(serverData, "drop myview");
		assertNull(db.cursorTran().getView("myview"));
	}

	@Test
	public void test_parse_eof() {
		Request.execute("create tmp (aField) key(aField)");
		assertNotNull(db.tables.get("tmp"));
		try {
			Request.execute(serverData, "drop tmp extra");
			fail("should have got an exception");
		} catch (SuException e) {
			assertEquals("syntax error at line 1: expected: EOF got: IDENTIFIER",
					e.toString());
		}
		assertNotNull(db.tables.get("tmp"));
	}

}
