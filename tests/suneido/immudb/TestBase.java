/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

public class TestBase extends suneido.intfc.database.TestBase {

	@Override
	protected suneido.intfc.database.DatabasePackage dbpkg() {
		return DatabasePackage.dbpkg;
	}

	ReadTransaction readTransaction() {
		return ((Database) db).readTransaction();
	}

	BulkTransaction bulkTransaction() {
		return ((Database) db).bulkTransaction();
	}

	UpdateTransaction updateTransaction() {
		return ((Database) db).updateTransaction();
	}

	@Override
	protected Record rec(Object... values) {
		return (Record) super.rec(values);
	}

}
