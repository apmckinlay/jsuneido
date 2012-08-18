package suneido.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BtreeIndexTest extends TestBase {
	@Test
	public void test_iter_range() {
		makeTable(10);
		Transaction t = db.readTransaction();
		Table tbl = t.getTable("test");
		try {
			BtreeIndex bi = t.getBtreeIndex(tbl.num, "a");
			BtreeIndex.Iter iter = bi.iter(key(3), key(6));
			for (int i = 3; i <= 6; ++i) {
				iter.next();
				assertEquals(i, iter.cur().key.getInt(0));
			}
			iter.next();
			assertTrue(iter.eof());

			iter = bi.iter(key(3), key(6));
			for (int i = 6; i >= 3; --i) {
				iter.prev();
				assertEquals(i, iter.cur().key.getInt(0));
			}
			iter.prev();
			assertTrue(iter.eof());
		} finally {
			t.complete();
		}
	}
}
