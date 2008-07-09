package suneido.database.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static suneido.database.Database.theDB;

import org.junit.Test;

import suneido.database.TestBase;

public class RequestTest extends TestBase {
	@Test
	public void test() {
		theDB = db;
		String schema = "(a,b,c) key(a)";
		Request.execute("create test " + schema);
		assertEquals(schema, db.schema("test"));

		Request.execute("ensure test (c,d,e) key(a) index(b,c)");
		schema = "(a,b,c,d,e) key(a) index(b,c)";
		assertEquals(schema, db.schema("test"));

		String extra = " index(c) in other(cc)";
		Request.execute("alter test create" + extra);
		assertEquals(schema + extra, db.schema("test"));

		Request.execute("alter test delete index(c)");
		assertEquals(schema, db.schema("test"));

		Request.execute("drop test");
		assertNull(db.getTable("test"));
	}
}
