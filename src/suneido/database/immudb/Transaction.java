/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;
import java.util.Map;

import javax.annotation.concurrent.NotThreadSafe;

import suneido.database.immudb.schema.Table;
import suneido.database.immudb.schema.Tables;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

@NotThreadSafe
public class Transaction {
	private final Storage stor;
	final Tran tran;
	final DbInfo dbinfo;
	Tables schema;
	final Map<String,Btree> indexes = Maps.newHashMap();

	public Transaction(Storage stor, int dbinfo, int redirs, Tables schema) {
		this.stor = stor;
		tran = new Tran(stor, redirs);
		this.dbinfo = new DbInfo(tran, dbinfo);
		this.schema = schema;
	}

	public Btree getIndex(String table) { // TODO handle multiple indexes
		Btree btree = indexes.get(table);
		if (btree != null)
			return btree;
		Table tbl = schema.get(table);
		TableInfo ti = dbinfo.get(tbl.num);
		btree = new Btree(tran, ti.firstIndex());
		indexes.put(table, btree);
		return btree;
	}

	// TODO synchronize
	public void commit() {
		tran.startStore();
		DataRecords.store(tran);
		Btree.store(tran);

		updateDbInfo();
		// TODO update nrows, totalsize

		int redirs = tran.storeRedirs();
		store(dbinfo.store(), redirs);
		tran.endStore();
	}

	private void updateDbInfo() {
		for (Map.Entry<String, Btree> e : indexes.entrySet()) {
			String tableName = e.getKey();
			int tblnum = schema.get(tableName).num;
			Btree btree = e.getValue();
			TableInfo ti = dbinfo.get(tblnum);
			ti = new TableInfo(tblnum, ti.nextfield, ti.nrows, ti.totalsize,
					ImmutableList.of(
							new IndexInfo(ti.firstIndex().columns, btree.info())));
			// TODO handle multiple indexes
			dbinfo.add(ti);
		}
	}

	static final int INT_SIZE = 4;

	// TODO remove duplication with Bootstrap
	private void store(int dbinfo, int redirs) {
		ByteBuffer buf = stor.buffer(stor.alloc(2 * INT_SIZE));
		buf.putInt(dbinfo);
		buf.putInt(redirs);
	}

}
