/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import org.junit.Test;

import suneido.intfc.database.IndexIter;

public class MergeIndexIterTest extends IndexIterTestBase {

	@Test
	public void simple_merge() {
		test(a(0, 2), a(1, 3), a(0, 1, 2, 3));
		test(a(0, 1), a(2, 3), a(0, 1, 2, 3));
		test(a(1, 2, 3), a(), a(1, 2, 3));
		test(a(), a(1, 2, 3), a(1, 2, 3));
		test(a(1, 3, 5), a(2, 4), a(1, 2, 3, 4, 5));
	}

	private static void test(int[] a1, int[] a2, int[] result) {
		checkNext(result, new MergeIndexIter(iter(a1), iter(a2)));
		checkPrev(result, new MergeIndexIter(iter(a1), iter(a2)));
	}

	@Test
	public void switch_direction2() {
		int[] data1 = a(1, 2, 3, 4);
		int[] data2 = a();
		MergeIndexIter iter = new MergeIndexIter(iter(data1), iter(data2));
		testPrev(iter, 4);
		testPrev(iter, 3);
		testPrev(iter, 2);
		testNext(iter, 3);
		testNext(iter, 4); // last
		testPrev(iter, 3);
	}

	@Test
	public void switch_direction() {
		switch_direction(a(0, 2), a(1, 3));
		switch_direction(a(0, 1), a(2, 3));
		switch_direction(a(0, 1, 2, 3), a());
		switch_direction(a(), a(0, 1, 2, 3));
	}

	private static void switch_direction(int[] a1, int[] a2) {
		IndexIter iter;

		iter = new MergeIndexIter(iter(a1), iter(a2));
		testNext(iter, 0);
		testPrev(iter, -1);
		testNext(iter, -1);

		iter = new MergeIndexIter(iter(a1), iter(a2));
		testPrev(iter, 3);
		testNext(iter, -1);
		testPrev(iter, -1);

		iter = new MergeIndexIter(iter(a1), iter(a2));
		testNext(iter, 0);
		testNext(iter, 1);
		testPrev(iter, 0);
		testPrev(iter, -1);
		testNext(iter, -1);

		iter = new MergeIndexIter(iter(a1), iter(a2));
		testPrev(iter, 3);
		testPrev(iter, 2);
		testNext(iter, 3);
		testNext(iter, -1);
		testPrev(iter, -1);

		iter = new MergeIndexIter(iter(a1), iter(a2));
		testNext(iter, 0);
		testNext(iter, 1);
		testNext(iter, 2);
		testPrev(iter, 1);
		testPrev(iter, 0);
		testPrev(iter, -1);
		testNext(iter, -1);

		iter = new MergeIndexIter(iter(a1), iter(a2));
		testPrev(iter, 3);
		testPrev(iter, 2);
		testPrev(iter, 1);
		testNext(iter, 2);
		testNext(iter, 3);
		testNext(iter, -1);
		testPrev(iter, -1);

		iter = new MergeIndexIter(iter(a1), iter(a2));
		testNext(iter, 0);
		testNext(iter, 1);
		testNext(iter, 2);
		testNext(iter, 3); // last
		testPrev(iter, 2);
		testPrev(iter, 1);
		testPrev(iter, 0);
		testPrev(iter, -1);
		testNext(iter, -1);

		iter = new MergeIndexIter(iter(a1), iter(a2));
		testPrev(iter, 3);
		testPrev(iter, 2);
		testPrev(iter, 1);
		testPrev(iter, 0); // first
		testNext(iter, 1);
		testNext(iter, 2);
		testNext(iter, 3);
		testNext(iter, -1);
		testPrev(iter, -1);

		iter = new MergeIndexIter(iter(a1), iter(a2));
		testNext(iter, 0);
		testNext(iter, 1);
		testNext(iter, 2);
		testNext(iter, 3);
		testNext(iter, -1); // stick at eof
		testPrev(iter, -1);

		iter = new MergeIndexIter(iter(a1), iter(a2));
		testPrev(iter, 3);
		testPrev(iter, 2);
		testPrev(iter, 1);
		testPrev(iter, 0);
		testPrev(iter, -1); // stick at eof
		testNext(iter, -1);
	}

}
