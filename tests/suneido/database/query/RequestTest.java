package suneido.database.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
		assertEquals(def, db.getView("myview"));
	}

	@Test(expected = SuException.class)
	public void test2() {
		Request.execute("ensure non_existant (a,b,c) index(a)");
	}

	@Test
	public void test_fkey() {
		test_fkey_create();
		Request.execute(serverData, "drop gl_accounts");
		Request.execute(serverData, "drop gl_tran1");
		Request.execute(serverData, "drop gl_tran2");
		test_fkey_create();
	}

	private void test_fkey_create() {
		Request.execute("ensure gl_accounts (glacct_num, glacct_abbrev)" +
				"key(glacct_num) index unique(glacct_abbrev)");
		Request.execute("ensure gl_tran1 (gltran1_num, glacct_num)" +
				"index(glacct_num) in gl_accounts key(gltran1_num)");
		Request.execute("ensure gl_tran2 (gltran2_num, glacct_num)" +
				"index(glacct_num) in gl_accounts key(gltran2_num)");
	}
}
