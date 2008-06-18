package suneido.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;
import java.util.TreeSet;

import org.junit.Test;

import suneido.SuInteger;
import suneido.SuString;
import static org.junit.Assert.*;

public class BtreeTest {
	@Test
	public void one_leaf() {
		Slot[] keys = { SlotTest.make("a"), SlotTest.make("m"), SlotTest.make("z") };
		Btree bt = new Btree(new DestMem());
		assertTrue(bt.isValid());
		assertTrue(bt.isEmpty());
		assertFalse(bt.iterator().hasNext());
		assertTrue(bt.insert(keys[1]));
		assertFalse(bt.insert(keys[1]));
		assertTrue(bt.insert(keys[0]));
		assertTrue(bt.insert(keys[2]));
		assertTrue(bt.isValid());
		assertFalse(bt.isEmpty());
		Iterator<Slot> iter = bt.iterator();
		for (Slot key : keys) {
			assertTrue(iter.hasNext());
			assertEquals(key, iter.next());
		}
		assertFalse(iter.hasNext());
	}
	
	@Test
	public void test() {
		Btree bt = maketree(100);		
		assertFalse(bt.isEmpty());
		int n = 0;
		for (Slot slot : bt)
			assertEquals(n++, slot.key.getLong(1));
		assertEquals(100, n);
		
		assertFalse(bt.erase(make(999)));
		ArrayList<Integer> v = shuffled(100, 123);
		for (int i : v)
			bt.erase(make(i));
		assertFalse(bt.erase(make(33)));
		assertTrue(bt.isEmpty());
		assertFalse(bt.iterator().hasNext());
		assertTrue(bt.isValid());
		}
	private Btree maketree(final int N) {
		Destination dest = new DestMem();
		Btree bt = new Btree(dest);
		// seed chosen to cover both 25% and 75% splits
		ArrayList<Integer> v = shuffled(N, 1);
		for (int i : v)
			assertTrue(bt.insert(new Slot(make(i))));
		assertTrue(bt.isValid());
		
		return new Btree(dest, bt.root(), bt.treelevels(), bt.nnodes());
	}
	
	@Test
	public void random() {
		TreeSet<Integer> ts = new TreeSet<Integer>();
		Btree bt = new Btree(new DestMem());
		Random rnd = new Random(123);
		final int N = 1000;
		for (int i = 0; i < N; ++i) {
			int x = rnd.nextInt(100);
			if ((rnd.nextInt() & 1) == 0)
				assertEquals(ts.add(x), bt.insert(new Slot(make(x))));				
			else
				assertEquals(ts.remove(x), bt.erase(make(x)));
			if (i % 10 == 0)
				assertTrue(bt.isValid());
		}
		Iterator<Slot> iter = bt.iterator();
		for (int x : ts) {
			assertTrue(iter.hasNext());
			assertEquals(x, iter.next().key.getLong(1));
		}
		assertFalse(iter.hasNext());
	}
	
	private ArrayList<Integer> shuffled(final int N, int seed) {
		ArrayList<Integer> v = new ArrayList<Integer>();
		for (int i = 0; i < N; ++i)
			v.add(i);
		// set seed so reproducible
		Random rnd = new Random(seed);
		Collections.shuffle(v, rnd);
		return v;
	}

	final private static SuString filler = new SuString("hellooooooooooooooooooooooooooooooooooooooooooo");
	public static BufRecord make(int num) {
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
		return r;
	}	
	
	public static void main(String args[]) {
		new BtreeTest().test();
	}
}
