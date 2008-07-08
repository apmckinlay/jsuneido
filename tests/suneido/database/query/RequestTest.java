package suneido.database.query;

import static org.junit.Assert.assertEquals;
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

		String more = " index(b,c)";
		Request.execute("alter test create" + more);
		assertEquals(schema + more, db.schema("test"));
	}
}
