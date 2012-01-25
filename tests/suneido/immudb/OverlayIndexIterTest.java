/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import org.junit.Test;

import suneido.intfc.database.IndexIter;

public class OverlayIndexIterTest extends IndexIterTestBase {

	@Test
	public void test() {
		IndexIter global = iter(1, 2, 3, 4);
		IndexIter local = iter(2, 3, 5);
		checkNext(a(1, 4, 5), new OverlayIndexIter(global, local));
	}

}
