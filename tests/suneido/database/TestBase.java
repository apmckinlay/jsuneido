/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

import org.junit.After;
import org.junit.Before;

public class TestBase extends TestBaseBase {

	@Before
	public void create() {
		dest = new DestMem();
		TheDb.set(new Database(dest, Mode.CREATE));
	}

	@After
	public void close() {
		TheDb.close();
	}

}