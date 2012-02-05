/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

interface ImmuExclTran extends ImmuUpdateTran {

	void addTableInfo(TableInfo tableInfo);

	void updateSchemaTable(Table table);

	void dropTableSchema(Table oldTable);

	void addSchemaTable(Table newTable);

}
