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

/**
 * Transactions should be thread contained.
 * They take a "snapshot" of the database state at the start
 * and then update the database state when they commit.
 * Storage is only written during commit.
 * Commit is single-threaded.
 */
@NotThreadSafe
public class Transaction {
	private final Database db;
	private final Storage stor;
	private final Tran tran;
	private final DbInfo dbinfo;
	private Tables schema;
	private final Tables original_schema;
	private final Map<String,Btree> indexes = Maps.newHashMap();

	public Transaction(Database db) {
		this.db = db;
		stor = db.stor;
		dbinfo = db.dbinfo.snapshot();
		schema = db.schema;
		original_schema = db.schema;
		tran = new Tran(stor, db.redirs);
	}

	public Btree getIndex(String table) { // TODO handle multiple indexes per table
		Btree btree = indexes.get(table);
		if (btree != null)
			return btree;
		Table tbl = schema.get(table);
		TableInfo ti = dbinfo.get(tbl.num);
		btree = new Btree(tran, ti.firstIndex());
		indexes.put(table, btree);
		return btree;
	}

	public boolean hasIndex(String table) {
		return indexes.containsKey(table);
	}

	public void addIndex(String table) {
		indexes.put(table, new Btree(tran));
	}

	public void addSchemaTable(Table table) {
		schema = schema.with(table);
	}

	public void addTableInfo(TableInfo ti) {
		dbinfo.add(ti);
	}

	// used by TableBuilder
	public void addRecord(Record r, String table, int... indexColumns) {
		Btree btree = getIndex(table);
		IndexedData id = new IndexedData().index(btree, indexColumns);
		id.add(tran, r);
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

		updateSchema();

		// TODO merge dbinfo and redirs with database
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

	private void store(int dbinfo, int redirs) {
		ByteBuffer buf = stor.buffer(stor.alloc(2 * INT_SIZE));
		buf.putInt(dbinfo);
		buf.putInt(redirs);
	}

	private void updateSchema() {
		assert db.schema == original_schema;
		db.schema = schema;
	}

}
