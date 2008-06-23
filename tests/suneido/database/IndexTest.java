package suneido.database;

import org.junit.Test;

import suneido.SuInteger;
import static org.junit.Assert.*;

public class IndexTest {
	private static class Visible implements Visibility {
		public boolean visible(int tran, long adr) {
			return true;
		}
	}
	
	@Test
	public void normal() {
		Index ix = new Index(new DestMem(), new Visible(), false, false);
		assertTrue(ix.insert(0, makeslot(123)));
		assertTrue(ix.insert(0, makeslot(123)));
	}
	
	@Test
	public void key() {
		Index ix = new Index(new DestMem(), new Visible(), true, false);
		assertTrue(ix.insert(0, makeslot()));
		assertFalse(ix.insert(0, makeslot()));
	}

	@Test
	public void unique() {
		Index ix = new Index(new DestMem(), new Visible(), false, true);
		assertTrue(ix.insert(0, makeslot()));
		assertTrue(ix.insert(0, makeslot()));
		
		assertTrue(ix.insert(0, makeslot(12, 34)));
		assertFalse(ix.insert(0, makeslot(12, 34)));
	}
	
	@Test
	public void next_prev() {
		Index ix = new Index(new DestMem(), new Visible(), false, false);
		for (int i = 0; i < 100; ++i)
			ix.insert(0, makeslot(i));
		
//		Index.Iter iter = ix.iter(makeslot(40), makeslot(60));
	}

	long next = 4;
	private Slot makeslot(int ... args) {
		BufRecord r = new BufRecord(100);
		for (int i : args)
			r.add(SuInteger.valueOf(i));
		r.addMmoffset(next += 4);
		return new Slot(r);
	}
}
