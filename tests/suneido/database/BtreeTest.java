package suneido.database;

import java.io.File;
import java.util.Iterator;

import org.junit.Test;
import static org.junit.Assert.*;

import suneido.SuException;

public class BtreeTest {
	@Test
	public void test1() {
		Slot[] keys = { SlotTest.make1("a"), SlotTest.make1("m"), SlotTest.make1("z") };
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
}
