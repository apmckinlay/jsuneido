/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

public class TestBase extends suneido.intfc.database.TestBase {

	@Override
	protected suneido.intfc.database.DatabasePackage dbpkg() {
		return DatabasePackage2.dbpkg;
	}

}
