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
			assertEquals(n++, slot.key.getLong(0));
		assertEquals(100, n);
		
		assertFalse(bt.erase(makerec(999)));
		ArrayList<Integer> v = shuffled(100, 123);
		for (int i : v)
			assertTrue("erasing " + i, bt.erase(makerec(i)));
		assertFalse(bt.erase(makerec(33)));
		assertTrue(bt.isEmpty());
		assertFalse(bt.iterator().hasNext());
		assertTrue(bt.isValid());
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
				assertEquals(ts.add(x), bt.insert(new Slot(makerec(x))));				
			else
				assertEquals(ts.remove(x), bt.erase(makerec(x)));
			if (i % 10 == 0)
				assertTrue(bt.isValid());
		}
		Iterator<Slot> iter = bt.iterator();
		for (int x : ts) {
			assertTrue(iter.hasNext());
			assertEquals(x, iter.next().key.getLong(0));
		}
		assertFalse(iter.hasNext());
	}
		
	@Test
	public void rangefrac_onelevel() {
		Btree bt = new Btree(new DestMem());
		assertfeq(0, bt.rangefrac(makerec(10, 0), makerec(20, 0)));
		
		bt = maketree(100, 0);
	
		assertEquals(0, bt.treelevels());
	
		assertfeq(1, bt.rangefrac(makerec(0, 0), endkey(99)));
		assertfeq(1, bt.rangefrac(emptykey, endkey(99)));
		assertfeq((float) .2, bt.rangefrac(emptykey, endkey(20)));
		assertfeq(1, bt.rangefrac(makerec(0, 0), endkey(999)));
		assertfeq(1, bt.rangefrac(emptykey, endkey(999)));
		assertfeq((float) .1, bt.rangefrac(makerec(10, 0), makerec(20, 0)));
		assertfeq((float) .01, bt.rangefrac(makerec(20, 0), endkey(20)));
		assertfeq(0, bt.rangefrac(emptykey, emptykey));
		assertfeq(0, bt.rangefrac(makerec(999, 0), endkey(999)));
	}
	private BufRecord endkey(int i) {
		BufRecord r = makerec(i, 0);
		r.addMax();
		return r;
	}
	@Test
	public void rangefrac_multilevel() {
		Btree bt = maketree(100);
		assertEquals(2, bt.treelevels());
	
		assertfeq(1, bt.rangefrac(makerec(0), endkey(99)));
		assertfeq((float) .1, bt.rangefrac(makerec(20), makerec(30)));
		assertfeq((float) .2, bt.rangefrac(makerec(0), makerec(20)));
		assertfeq((float) .01, bt.rangefrac(makerec(20), endkey(20)));
		assertfeq(0, bt.rangefrac(emptykey, emptykey));
		assertfeq(0, bt.rangefrac(makerec(999), endkey(999)));
	}
	private void assertfeq(float x, float y) {
		assertEquals(x, y, .05);
	}
	final private static BufRecord emptykey = new BufRecord(10);

	final private static int NFILLER = 10;
	private Btree maketree(final int N) {
		return maketree(N, NFILLER);
	}
	private Btree maketree(final int N, int nfiller) {
		Destination dest = new DestMem();
		Btree bt = new Btree(dest);
		// seed chosen to cover both 25% and 75% splits
		ArrayList<Integer> v = shuffled(N, 1);
		for (int i : v)
			assertTrue(bt.insert(new Slot(makerec(i, nfiller))));
		assertTrue(bt.isValid());
		
		return new Btree(dest, bt.root(), bt.treelevels(), bt.nnodes());
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

	private static BufRecord makerec(int num) {
		return makerec(num, NFILLER);
	}
	private static BufRecord makerec(int num, int nfiller) {
		BufRecord r = new BufRecord(1000);
		r.add(SuInteger.valueOf(num));
		for (int i = 0; i < nfiller; ++i)
			r.add(filler);
		return r;
	}
	final private static SuString filler = new SuString("hellooooooooooooooooooooooooooooooooooooooooooo");
	
//	public static void main(String args[]) {
//		new BtreeTest().test();
//	}
}
