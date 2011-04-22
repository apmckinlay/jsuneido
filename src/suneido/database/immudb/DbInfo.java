/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;


public class DbInfo {
	private final DbHashTree tableInfo;

	public DbInfo(Tran tran, TableInfo... info) {
		DbHashTree tableInfo = DbHashTree.empty(tran.context);
		for (TableInfo ti : info)
			tableInfo = tableInfo.with(ti.tblnum, tran.refToInt(info));
		this.tableInfo = tableInfo;
	}

}
