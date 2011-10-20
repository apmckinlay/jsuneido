/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.language;

import static suneido.language.ExecuteTest.test;

import org.junit.Test;

import suneido.Suneido;

public class DatabaseTest {

	@Test
	public void x_delete() {
		Suneido.openDbms();
		test("try Database('drop tmp')\n" +
				"Database('create tmp (a,b,c) key(a)')\n" +
				"Transaction(update:)\n" +
				"{|t|\n" +
				"t.QueryOutput('tmp', [a: 1, b: 2])\n" +
				"t.QueryFirst('tmp').Delete()\n" +
				"}",
				"true");
	}

}
