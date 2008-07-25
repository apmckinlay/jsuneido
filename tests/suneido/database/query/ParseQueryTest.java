package suneido.database.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.database.TestBase;

//TODO parse object constants

public class ParseQueryTest extends TestBase {
	@Test
	public void test() {
		makeTable();
		makeTable("test2", "x", "y");
		makeTable("compat", "a", "b");
		makeTable("joinable", "x", "y", "a");

		String[] cases = {
			"test", null,
			"test SORT a,b", null,
			"test SORT REVERSE a,b", null,
			"test RENAME a to aa, b to bb", null,
			"test RENAME a to aa RENAME b to bb", null,
			"test RENAME a to aa, b to bb SORT a,b", null,
			"test PROJECT a,b", null,
			"test PROJECT b", null,
			"test REMOVE b",
				"test PROJECT a",
			"test REMOVE a",
				"test PROJECT b",
			"(test TIMES test2)", null,
			"(test MINUS compat)", null,
			"(test UNION compat)", null,
			"(test INTERSECT compat)", null,
			"history(test)", null,
			"test JOIN joinable",
				"(test JOIN 1:n on (a) joinable)",
			"test LEFTJOIN joinable",
				"(test LEFTJOIN 1:n on (a) joinable)",
			"test SUMMARIZE b, count, total a, x = max b",
				"test SUMMARIZE (b) count, total a, x = max b",
			"test WHERE !a", null,
			"test WHERE (a * 5)", null,
			"test WHERE (a - 5)", null,
			"test WHERE (a >> 2)", null,
			"test WHERE (a > 5)", null,
			"test WHERE (a = b)", null,
			"test WHERE (a & 4)", null, "test WHERE (a ^ 2)", null,
			"test WHERE (a | 1)", null,
			"test WHERE (a and b)", null,
			"test WHERE (a and b and c)", null,
			"test WHERE (a or b)", null,
			"test WHERE (a or b or c)", null,
			"test WHERE (a ? b : 5)", null,
			"test WHERE a in (2,3,4)",
				null,
			"test EXTEND Z, x = 12, y = (b + a), f = fn(), g = fn(1), h = fn(1,2,3)",
				null,
			"DELETE test WHERE (a = 5)", null,
			"UPDATE test SET a=5, b=3", null,
			"INSERT [a: 4, b: 6] INTO test", null,
		};
		for (int i = 0; i < cases.length; i += 2) {
			String s = cases[i];
			String expect = cases[i + 1] == null ? s : cases[i + 1];
			assertEquals(s, expect, ParseQuery.parse(s).toString());
		}
	}

	private void makeTable(String tablename, String... columns) {
		db.addTable(tablename);
		for (String column : columns)
			db.addColumn(tablename, column);
		db.addIndex(tablename, columns[0], true);
	}
}
