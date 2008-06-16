package suneido.database;

import java.util.Iterator;
import org.junit.Test;
import static org.junit.Assert.*;

public class BtreeTest {
	@Test
	public void one_leaf() {
		Slot[] keys = { SlotTest.make("a"), SlotTest.make("m"), SlotTest.make("z") };
		Btree bt = new Btree(new DestMem());
		assertTrue(bt.insert(keys[1]));
		assertFalse(bt.insert(keys[1]));
		assertTrue(bt.insert(keys[0]));
		assertTrue(bt.insert(keys[2]));
		Iterator<Slot> iter = bt.iterator();
		for (Slot key : keys) {
			assertTrue(iter.hasNext());
			assertEquals(key, iter.next());
		}
		assertFalse(iter.hasNext());
	}
	
	@Test
	public void split() {
		Btree bt = new Btree(new DestMem());
		for (int i = 0; i < 130; ++i)
			assertTrue(bt.insert(SlotTest.make(i)));
		int n = 0;
		for (Slot slot : bt)
			assertEquals(n++, slot.key.getLong(1));
	}

	public static void main(String args[]) {
		new BtreeTest().split();
	}
}
