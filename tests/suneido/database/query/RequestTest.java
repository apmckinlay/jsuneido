package suneido.database.query;

import static org.junit.Assert.*;

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

		String extra = " index(c) in other(cc)";
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
	public void test_view() {
		String def = "/*comment*/ one join two where three = 4";
		Request.execute(db, serverData, "view myview = " + def);
		Transaction t = db.readonlyTran();
		assertEquals(def, t.getView("myview"));
		t.complete();
		Request.execute(db, serverData, "drop myview");
		t = db.readonlyTran();
		assertNull(t.getView("myview"));
		t.complete();
	}

	@Test
	public void test_parse_eof() {
		Request.execute(db, "create tmp (aField) key(aField)");
		assertNotNull(db.getSchema("tmp"));
		try {
			Request.execute(db, serverData, "drop tmp extra");
			fail("should have got an exception");
		} catch (SuException e) {
			assertEquals("syntax error at line 1: expected: EOF got: IDENTIFIER",
					e.toString());
		}
		assertNotNull(db.getSchema("tmp"));
	}

}
