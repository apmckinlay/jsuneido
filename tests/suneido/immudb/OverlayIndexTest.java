/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import gnu.trove.set.hash.TIntHashSet;

import org.junit.Test;

public class OverlayIndexTest extends IndexIterTestBase {

	@Test
	public void handle_deletes() {
		Storage stor = new MemStorage(1024, 64);
		Tran tran = new Tran(stor);
		Btree2 local = new Btree2(tran);
		Btree2 global = new Btree2(tran);
		assertTrue(global.add(key("a", 1), true));
		assertTrue(global.add(key("b", 2), true));
		assertTrue(global.add(key("c", 3), true));
		TIntHashSet deletes = new TIntHashSet();
		OverlayIndex index = new OverlayIndex(global, local, deletes);
		assertEquals(1, index.get(rec("a")));
		assertEquals(2, index.get(rec("b")));
		assertEquals(3, index.get(rec("c")));

		int d = tran.refToInt(new Object());
		assertTrue(index.add(key("d", d), true));
		assertEquals(d, index.get(rec("d")));

		assertTrue(index.remove(key("b", 2)));
		deletes.add(2);
		assertEquals(0, index.get(rec("b")));

		int b = tran.refToInt(new Object());
		assertTrue(index.add(key("b", b), true)); // new version of b
		assertEquals(b, index.get(rec("b")));

		assertTrue(index.remove(key("d", d)));
		assertEquals(0, index.get(rec("d")));
	}

	private static Record key(String s, int adr) {
		return new RecordBuilder().add(s).adduint(adr).build();
	}

	private static Record rec(String s) {
		return new RecordBuilder().add(s).build();
	}

	@Test
	public void test() {
		checkNext(a(1, 2, 3, 4, 5),
				new OverlayIndex.Iter(iter(1, 2, 3, 4), iter(5), dels()));
		checkPrev(a(1, 2, 3, 4, 5),
				new OverlayIndex.Iter(iter(1, 2, 3, 4), iter(5), dels()));
		checkNext(a(1, 4, 5),
				new OverlayIndex.Iter(iter(1, 2, 3, 4), iter(5), dels(2, 3)));
		checkPrev(a(1, 4, 5),
				new OverlayIndex.Iter(iter(1, 2, 3, 4), iter(5), dels(2, 3)));
	}

	TIntHashSet dels(int... values) {
		TIntHashSet dels = new TIntHashSet();
		dels.addAll(values);
		return dels;
	}

}
