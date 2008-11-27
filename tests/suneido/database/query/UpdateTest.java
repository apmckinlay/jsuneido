package suneido.database.query;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import suneido.database.Record;
import suneido.database.TestBase;

public class UpdateTest extends TestBase {

	@Test
	public void test() {
		makeTable(4);

		assertEquals(4, get("test").size());
		QueryAction q = (QueryAction) ParseQuery
				.parse("update test where a >= 1 and a <= 2 set b = 'xxx'");
		assertEquals(2, q.execute());
		List<Record> recs = get("test");
		assertEquals(4, recs.size());
		assertEquals(record(0), recs.get(0));
		assertEquals(record(3), recs.get(3));
		assertEquals(new Record().add(1).add("xxx"), recs.get(1));
		assertEquals(new Record().add(2).add("xxx"), recs.get(2));
	}

}
