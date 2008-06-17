package suneido.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

import org.junit.Test;

import suneido.SuInteger;
import suneido.SuString;
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
		Destination dest = new DestMem();
		Btree bt = new Btree(dest);
		final int N = 100;
		ArrayList<Integer> v = new ArrayList<Integer>();
		for (int i = 0; i < N; ++i)
			v.add(i);
		// set seed so test is reproducible
		// seed chosen to cover both 25% and 75% splits
		Random rnd = new Random(1);
		Collections.shuffle(v, rnd);
		for (int i : v)
			assertTrue(bt.insert(make(i)));
		bt = new Btree(dest, bt.root(), bt.treelevels(), bt.nnodes());		
		int n = 0;
		for (Slot slot : bt)
			assertEquals(n++, slot.key.getLong(1));
	}

	final private static SuString filler = new SuString("hellooooooooooooooooooooooooooooooooooooooooooo");
	public static Slot make(int num) {
		BufRecord r = new BufRecord(1000);
		r.add(filler);
		r.add(SuInteger.valueOf(num));
		r.add(filler);
		r.add(filler);
		r.add(filler);
		r.add(filler);
		r.add(filler);
		r.add(filler);
		r.add(filler);
		r.add(filler);
		r.add(filler);
		return new Slot(r);
	}
	
	public static void main(String args[]) {
		new BtreeTest().split();
	}
}
