/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.util.Set;

interface ImmuReadTran extends suneido.intfc.database.Transaction {

	@Override
	Table getTable(int tblnum);

	@Override
	Table getTable(String tblname);

	@Override
	Table ck_getTable(String tblname);

	TranIndex getIndex(int tblnum, int... colNums);

	boolean hasIndex(int tblnum, int[] colNums);

	boolean exists(int num, int[] namesToNums, Record key);

	@Override
	public Record input(int adr);

	ImmuExclTran exclusiveTran();

	Set<ForeignKeyTarget> getForeignKeys(String tableName, String colNames);

	TableInfo getTableInfo(int tblnum);

	Tran tran();

	Record lookup(int views, int[] indexCols, Record key);

}
