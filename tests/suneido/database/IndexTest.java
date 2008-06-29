package suneido.database;

import org.junit.Test;

import suneido.SuInteger;
import static org.junit.Assert.*;

public class IndexTest {
	@Test
	public void normal() {
		BtreeIndex ix = new BtreeIndex(dest = new DestMem(), 0, "", false, false);
		assertTrue(ix.insert(0, makeslot(123)));
		assertTrue(ix.insert(0, makeslot(123)));
	}
	
	@Test
	public void key() {
		BtreeIndex ix = new BtreeIndex(dest = new DestMem(), 0, "", true, false);
		assertTrue(ix.insert(0, makeslot()));
		assertFalse(ix.insert(0, makeslot()));
	}

	@Test
	public void unique() {
		BtreeIndex ix = new BtreeIndex(dest = new DestMem(), 0, "", false, true);
		assertTrue(ix.insert(0, makeslot()));
		assertTrue(ix.insert(0, makeslot()));
		
		assertTrue(ix.insert(0, makeslot(12, 34)));
		assertFalse(ix.insert(0, makeslot(12, 34)));
	}
	
	@Test
	public void next_prev() {
		BtreeIndex ix = new BtreeIndex(dest = new DestMem(), 0, "", false, false);
		int i;
		for (i = 0; i < 100; ++i)
			ix.insert(0, makeslot(i));
		
		BtreeIndex.Iter iter = ix.iter(0, makekey(40), makekey(60));
		for (i = 40; i <= 60; ++i)
			assertEquals(i, iter.next().cur().key.getLong(0));
		assertTrue(iter.next().eof());
		
		iter = ix.iter(0, makekey(40), makekey(60));
		for (i = 60; i >= 40; --i)
			assertEquals(i, iter.prev().cur().key.getLong(0));
		assertTrue(iter.prev().eof());
	}

	private DestMem dest;
	private Slot makeslot(int ... args) {
		Record r = new Record(100);
		for (int i : args)
			r.add(i);
		r.addMmoffset(dest.alloc(0));
		return new Slot(r);
	}
	
	private Record makekey(int ... args) {
		Record r = new Record(100);
		for (int i : args)
			r.add(i);
		return r;
	}

//	public static void main(String args[]) {
//		new IndexTest().next_prev();
//	}
}
