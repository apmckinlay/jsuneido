package suneido.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static suneido.database.Database.theDB;

import org.junit.Test;


public class BtreeIndexTest extends TestBase {
	@Test
	public void test() {
		makeTable(10);
		Table tbl = theDB.getTable("test");
		BtreeIndex bi = tbl.getIndex("a").btreeIndex;
		Transaction t = theDB.readonlyTran();
		try {
			BtreeIndex.Iter iter = bi.iter(t, key(3), key(6));
			for (int i = 3; i <= 6; ++i) {
				iter.next();
				assertEquals(i, iter.cur().key.getLong(0));
			}
			iter.next();
			assertTrue(iter.eof());
		} finally {
			t.complete();
		}
	}
}
