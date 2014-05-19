/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import org.junit.Test;

public class SimpleIndexIterTest extends IndexIterTestBase {

	@Test
	public void testSimpleIndexIter() {
		testNext(iter(1, 2, 3), 1);
		testPrev(iter(1, 2, 3), 3);
	}

}
