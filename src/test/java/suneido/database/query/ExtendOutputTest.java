/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.query;

import org.junit.Test;

public class ExtendOutputTest extends TestBase {

	@Test
	public void test() {
		makeTable();
		req("insert { a: 1, b: 2, e: 5 } into test");
	}

}
