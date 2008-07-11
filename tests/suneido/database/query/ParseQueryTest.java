package suneido.database.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class ParseQueryTest {
	@Test
	public void test() {
		Query q = ParseQuery.parse("test");
		assertEquals("test", q.toString());

		q = ParseQuery.parse("(test)");
		assertEquals("test", q.toString());

		q = ParseQuery.parse("test sort a,b");
		assertEquals("test SORT a,b", q.toString());
	}
}
