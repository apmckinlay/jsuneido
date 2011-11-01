package suneido.database.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DeleteTest extends TestBase {

	@Test
	public void delete_all() {
		makeTable(3);

		assertEquals(3, get("test").size());
		assertEquals(3, req("delete test"));
		assertEquals(0, get("test").size());
	}

	@Test
	public void delete_selected() {
		makeTable(5);

		assertEquals(5, get("test").size());
		assertEquals(2, req("delete test where a >= 1 and a <= 2 "));
		assertEquals(3, get("test").size());
	}

}
