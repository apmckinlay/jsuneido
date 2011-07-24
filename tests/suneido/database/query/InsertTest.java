package suneido.database.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class InsertTest extends TestBase {

	@Test
	public void test() {
		makeTable();
		
		assertEquals(0, get("test").size());
		assertEquals(1,  req("insert [a: 3, b: 'more stuff'] into test"));
		check(3);
	}

}
