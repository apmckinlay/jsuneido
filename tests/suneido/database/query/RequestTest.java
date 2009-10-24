package suneido.database.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import suneido.SuException;
import suneido.database.TestBase;

public class RequestTest extends TestBase {
	@Test
	public void test() {
		test_();
		test_();
	}

	private void test_() {
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
		assertNull(db.getTable("test"));

		Request.execute(serverData, "drop tmp");
		assertNull(db.getTable("tmp"));

		Request.execute("create tmp (aField) key(aField)");

		Request.execute(serverData, "drop tmp");
		assertNull(db.getTable("tmp"));
	}

	@Test
	public void test_view() {
		String def = "one join two where three = 4";
		Request.execute(serverData, "view myview = " + def);
		assertEquals(def, db.getView("myview"));
	}

	@Test(expected = SuException.class)
	public void test2() {
		Request.execute("ensure non_existant (a,b,c) index(a)");
	}
}
