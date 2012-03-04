package suneido.database.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suneido.intfc.database.Transaction;

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
		makeTable(100);

		assertEquals(100, get("test").size());
		assertEquals(10, req("delete test where a >= 80 and a < 90 "));
		assertEquals(90, get("test").size());

		Transaction t = db.updateTransaction();
		for (int i = 80; i < 90; ++i)
			t.addRecord("test", record(i));
		t.ck_complete();

		assertEquals(100, get("test").size());
		assertEquals(10, req("delete test where a >= 80 and a < 90 "));
		assertEquals(90, get("test").size());
	}

}
