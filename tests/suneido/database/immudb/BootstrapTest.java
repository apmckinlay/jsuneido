/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import org.junit.Test;

public class BootstrapTest {

	@Test
	public void test() {
		TestStorage stor = new TestStorage(500, 100);
		Bootstrap bs = new Bootstrap(stor);
		bs.create();

		Database db = new Database(stor);
		db.open();
	}

}
