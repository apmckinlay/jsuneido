package suneido.database.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.database.TestBase;


public class ParseQueryTest extends TestBase {
	@Test
	public void test() {
		makeTable();
		String[] cases = {
				"test", null, "test SORT a,b", null,
				"test SORT REVERSE a,b", null, "test RENAME a to aa, b to bb",
				null, "test RENAME a to aa, b to bb SORT a,b", null,
				"test PROJECT a,b", "test PROJECT-COPY a,b",
				"test PROJECT b", null,
				"test REMOVE b", "test PROJECT-COPY a",
				"test REMOVE a",
				"test PROJECT b"

		};
		for (int i = 0; i < cases.length; i += 2) {
			String s = cases[i];
			String expect = cases[i + 1] == null ? s : cases[i + 1];
			assertEquals(expect, ParseQuery.parse(s).toString());
		}
	}
}
