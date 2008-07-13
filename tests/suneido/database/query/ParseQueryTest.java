package suneido.database.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class ParseQueryTest {
	@Test
	public void test() {
		String[] cases = {
				"test",
				"test SORT a,b",
				"test SORT REVERSE a,b",
				"test RENAME a to aa, b to bb",
				"test RENAME a to aa, b to bb SORT a,b"
		};
		for (String s : cases)
			assertEquals(s, ParseQuery.parse(s).toString());
	}
}
