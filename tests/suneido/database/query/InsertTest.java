package suneido.database.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.database.TestBase;

public class InsertTest extends TestBase {

	@Test
	public void test() {
		makeTable();

		assertEquals(0, get("test").size());
		QueryAction q = (QueryAction) ParseQuery
				.parse("insert [a: 3, b: 'more stuff'] into test");
		assertEquals(1, q.execute());
		check(3);
	}

}
