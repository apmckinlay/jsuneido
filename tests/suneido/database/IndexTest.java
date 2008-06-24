package suneido.database;

import org.junit.Test;

import suneido.SuInteger;
import static org.junit.Assert.*;

public class IndexTest {
	private static class Visible implements Visibility {
		public boolean visible(int tran, long adr) {
			return true;
		}
		public TranRead read_act(int tran, int tblnum, String index) {
			// TODO Auto-generated method stub
			return new TranRead(tblnum, index);
		}
	}
	
	@Test
	public void normal() {
		Index ix = new Index(dest = new DestMem(), new Visible(), 0, "", false, false);
		assertTrue(ix.insert(0, makeslot(123)));
		assertTrue(ix.insert(0, makeslot(123)));
	}
	
	@Test
	public void key() {
		Index ix = new Index(dest = new DestMem(), new Visible(), 0, "", true, false);
		assertTrue(ix.insert(0, makeslot()));
		assertFalse(ix.insert(0, makeslot()));
	}

	@Test
	public void unique() {
		Index ix = new Index(dest = new DestMem(), new Visible(), 0, "", false, true);
		assertTrue(ix.insert(0, makeslot()));
		assertTrue(ix.insert(0, makeslot()));
		
		assertTrue(ix.insert(0, makeslot(12, 34)));
		assertFalse(ix.insert(0, makeslot(12, 34)));
	}
	
	@Test
	public void next_prev() {
		Index ix = new Index(dest = new DestMem(), new Visible(), 0, "", false, false);
		int i;
		for (i = 0; i < 100; ++i)
			ix.insert(0, makeslot(i));
		
		Index.Iter iter = ix.iter(0, makekey(40), makekey(60));
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
		BufRecord r = new BufRecord(100);
		for (int i : args)
			r.add(SuInteger.valueOf(i));
		r.addMmoffset(dest.alloc(0));
		return new Slot(r);
	}
	
	private BufRecord makekey(int ... args) {
		BufRecord r = new BufRecord(100);
		for (int i : args)
			r.add(SuInteger.valueOf(i));
		return r;
	}

	public static void main(String args[]) {
		new IndexTest().next_prev();
	}
}
