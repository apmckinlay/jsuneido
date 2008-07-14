package suneido.database.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.database.TestBase;


public class ParseQueryTest extends TestBase {
	@Test
	public void test() {
		makeTable();
		makeTable("test2", "x", "y");
		makeTable("compat", "a", "b");

		String[] cases = {
				"test", null,
				"test SORT a,b", null,
				"test SORT REVERSE a,b", null,
				"test RENAME a to aa, b to bb", null,
				"test RENAME a to aa, b to bb SORT a,b", null,
				"test PROJECT a,b", "test PROJECT-COPY a,b",
				"test PROJECT b", null,
				"test REMOVE b", "test PROJECT-COPY a",
				"test REMOVE a", "test PROJECT b",
				"(test) TIMES (test2)", null,
				"(test) MINUS (compat)", null,
				"(test) UNION (compat)", null,
				"(test) INTERSECT (compat)", null

		};
		for (int i = 0; i < cases.length; i += 2) {
			String s = cases[i];
			String expect = cases[i + 1] == null ? s : cases[i + 1];
			assertEquals(expect, ParseQuery.parse(s).toString());
		}
	}

	private void makeTable(String tablename, String... columns) {
		db.addTable(tablename);
		for (String column : columns)
			db.addColumn(tablename, column);
		db.addIndex(tablename, columns[0], true);
	}
}
