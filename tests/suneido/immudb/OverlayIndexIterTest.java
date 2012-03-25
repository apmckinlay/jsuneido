/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import gnu.trove.set.hash.TIntHashSet;

import org.junit.Test;

public class OverlayIndexIterTest extends IndexIterTestBase {

	@Test
	public void test() {
		checkNext(a(1, 2, 3, 4, 5),
				new OverlayIndexIter(iter(1, 2, 3, 4), iter(5), dels()));
		checkPrev(a(1, 2, 3, 4, 5),
				new OverlayIndexIter(iter(1, 2, 3, 4), iter(5), dels()));
		checkNext(a(1, 4, 5),
				new OverlayIndexIter(iter(1, 2, 3, 4), iter(5), dels(2, 3)));
		checkPrev(a(1, 4, 5),
				new OverlayIndexIter(iter(1, 2, 3, 4), iter(5), dels(2, 3)));
	}

	TIntHashSet dels(int... values) {
		TIntHashSet dels = new TIntHashSet();
		dels.addAll(values);
		return dels;
	}

}
