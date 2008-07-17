package suneido.database.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.database.TestBase;

public class DeleteTest extends TestBase {

	@Test
	public void test() {
		makeTable(3);

		assertEquals(3, get("test").size());
		QueryAction q = (QueryAction) ParseQuery.parse("delete test");
		assertEquals(3, q.execute());
		assertEquals(0, get("test").size());
	}

}
