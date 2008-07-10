package suneido.database.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class ParseQueryTest {
	@Test
	public void test() {
		QueryTable qt = (QueryTable) ParseQuery.parse("test");
		assertEquals("test", qt.toString());
	}
}
