/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;
import java.util.Map;

import javax.annotation.concurrent.NotThreadSafe;

import suneido.database.immudb.schema.Table;
import suneido.database.immudb.schema.Tables;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;

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
	private final HashBasedTable<String,String,Btree> indexes = HashBasedTable.create();

	public Transaction(Database db) {
		this.db = db;
		stor = db.stor;
		dbinfo = new DbInfo(stor, db.dbinfo);
		schema = db.schema;
		original_schema = db.schema;
		tran = new Tran(stor, new Redirects(db.redirs));
	}

	public Btree getIndex(String table, int... indexColumns) {
		return getIndex(table, Ints.join(",", indexColumns));
	}

	/** indexColumns are like "3,4" */
	public Btree getIndex(String table, String indexColumns) {
		Btree btree = indexes.get(table, indexColumns);
		if (btree != null)
			return btree;
		Table tbl = schema.get(table);
		TableInfo ti = dbinfo.get(tbl.num);
		btree = new Btree(tran, ti.getIndex(indexColumns));
		indexes.put(table, indexColumns, btree);
		return btree;
	}

	public boolean hasIndex(String table, String indexColumns) {
		return indexes.contains(table, indexColumns);
	}

	public void addIndex(String table, String indexColumns) {
		indexes.put(table, indexColumns, new Btree(tran));
	}

	public void addSchemaTable(Table table) {
		schema = schema.with(table);
	}

	public void addTableInfo(TableInfo ti) {
		dbinfo.add(ti);
	}

	// used by TableBuilder
	public void addRecord(Record r, String table, int... indexColumns) {
		Btree btree = getIndex(table, indexColumns);
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
		for (String table : indexes.rowKeySet()) {
			int tblnum = schema.get(table).num;
			TableInfo ti = dbinfo.get(tblnum);
			Map<String,Btree> idxs = indexes.row(table);
			ImmutableList.Builder<IndexInfo> b = ImmutableList.builder();
			for (IndexInfo ii : ti.indexInfo) {
				Btree btree = idxs.get(ii.columns);
				b.add((btree == null)
						? ii : new IndexInfo(ii.columns, btree.info()));
			}
			ti = new TableInfo(tblnum, ti.nextfield, ti.nrows, ti.totalsize,
					b.build());
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
		if (schema == original_schema)
			return; // no schema changes in this transaction

		// schema changes must be done while holding the commit lock
		// so there should be no concurrent changes
		assert db.schema == original_schema;

		db.schema = schema;
	}

}
