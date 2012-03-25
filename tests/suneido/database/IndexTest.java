package suneido.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static suneido.database.Transaction.NULLTRAN;

import org.junit.Test;

public class IndexTest {
	private DestMem dest;

	@Test
	public void normal() {
		BtreeIndex ix = new BtreeIndex(dest = new DestMem(), 0, "", false, false);
		assertTrue(ix.insert(NULLTRAN, makeslot(123)));
		assertTrue(ix.insert(NULLTRAN, makeslot(123)));
	}

	@Test
	public void key() {
		BtreeIndex ix = new BtreeIndex(dest = new DestMem(), 0, "", true, false);
		assertTrue(ix.insert(NULLTRAN, makeslot()));
		assertFalse(ix.insert(NULLTRAN, makeslot()));
	}

	@Test
	public void unique() {
		BtreeIndex ix = new BtreeIndex(dest = new DestMem(), 0, "", false, true);
		assertTrue(ix.insert(NULLTRAN, makeslot()));
		assertTrue(ix.insert(NULLTRAN, makeslot()));

		assertTrue(ix.insert(NULLTRAN, makeslot(12, 34)));
		assertFalse(ix.insert(NULLTRAN, makeslot(12, 34)));
	}

	@Test
	public void next_prev() {
		BtreeIndex ix = new BtreeIndex(dest = new DestMem(), 0, "", false, false);
		int i;
		for (i = 0; i < 100; ++i)
			ix.insert(NULLTRAN, makeslot(i));

		BtreeIndex.Iter iter = ix.iter(makekey(40), makekey(60));
		for (i = 40; i <= 60; ++i) {
			iter.next();
			assertEquals(i, iter.cur().key.getInt(0));
		}
		iter.next();
		assertTrue(iter.eof());

		iter = ix.iter(makekey(40), makekey(60));
		for (i = 60; i >= 40; --i) {
			iter.prev();
			assertEquals(i, iter.cur().key.getInt(0));
		}
		iter.prev();
		assertTrue(iter.eof());
	}

	private Slot makeslot(int ... args) {
		Record r = new Record(100);
		for (int i : args)
			r.add(i);
		r.addMmoffset(dest.alloc(0, Mmfile.OTHER));
		return new Slot(r);
	}

	private static Record makekey(int ... args) {
		Record r = new Record(100);
		for (int i : args)
			r.add(i);
		return r;
	}

}
