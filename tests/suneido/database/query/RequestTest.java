package suneido.database.query;

import static org.junit.Assert.*;

import org.junit.Test;

import suneido.SuException;
import suneido.database.TestBase;
import suneido.database.Transaction;

public class RequestTest extends TestBase {
	@Test
	public void test() {
		String schema = "(a,b,c) key(a)";
		Request.execute(db, "create test " + schema);
		assertEquals(schema, db.getTable("test").schema());

		Request.execute(db, "ensure test2 " + schema);
		assertEquals(schema, db.getTable("test2").schema());

		Request.execute(db, "ensure test (c,d,e) KEY(a) INDEX(b,c)");
		schema = "(a,b,c,d,e) key(a) index(b,c)";
		assertEquals(schema, db.getTable("test").schema());

		String extra = " index(c) in other(cc)";
		Request.execute(db, "alter test create" + extra);
		assertEquals(schema + extra, db.getTable("test").schema());

		Request.execute(db, "ALTER test DROP index(c)");
		assertEquals(schema, db.getTable("test").schema());

		Request.execute(db, "alter test rename b to bb");
		schema = "(a,bb,c,d,e) key(a) index(bb,c)";
		assertEquals(schema, db.getTable("test").schema());

		Request.execute(db, "alter test drop (d,e)");
		schema = "(a,bb,c) key(a) index(bb,c)";
		assertEquals(schema, db.getTable("test").schema());

		Request.execute(db, "RENAME test TO tmp");
		assertEquals(schema, db.getTable("tmp").schema());
		assertNull(db.getTable("test"));

		Request.execute(db, serverData, "drop tmp");
		assertNull(db.getTable("tmp"));

		Request.execute(db, "create tmp (aField) key(aField)");

		Request.execute(db, serverData, "drop tmp");
		assertNull(db.getTable("tmp"));
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
		assertNotNull(db.getTable("tmp"));
		try {
			Request.execute(db, serverData, "drop tmp extra");
			fail("should have got an exception");
		} catch (SuException e) {
			assertEquals("syntax error at line 1: expected: EOF got: IDENTIFIER",
					e.toString());
		}
		assertNotNull(db.getTable("tmp"));
	}

}
