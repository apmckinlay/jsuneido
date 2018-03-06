/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

public class TestBase extends suneido.intfc.database.TestBase {

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
	protected DataRecord rec(Object... values) {
		return (DataRecord) super.rec(values);
	}

}
