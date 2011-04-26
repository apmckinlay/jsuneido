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
	private final Map<String, Map<String,Btree>> indexes = Maps.newHashMap();

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
		Map<String,Btree> idxs = indexes.get(table);
		if (idxs != null) {
			Btree btree = idxs.get(indexColumns);
			if (btree != null)
				return btree;
		}
		Table tbl = schema.get(table);
		TableInfo ti = dbinfo.get(tbl.num);
		Btree btree = new Btree(tran, ti.getIndex(indexColumns));
		if (idxs == null)
			idxs = Maps.newHashMap();
		idxs.put(indexColumns, btree);
		indexes.put(table, idxs);
		return btree;
	}

	public boolean hasIndex(String table, String indexColumns) {
		Map<String,Btree> idxs = indexes.get(table);
		return idxs != null && idxs.containsKey(indexColumns);
	}

	public void addIndex(String table, String indexColumns) {
		Map<String,Btree> idxs = indexes.get(table);
		if (idxs == null)
			idxs = Maps.newHashMap();
		idxs.put(indexColumns, new Btree(tran));
		indexes.put(table, idxs);
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
		for (Map.Entry<String, Map<String,Btree>> e : indexes.entrySet()) {
			String tableName = e.getKey();
			int tblnum = schema.get(tableName).num;
			TableInfo ti = dbinfo.get(tblnum);
			Map<String,Btree> idxs = e.getValue();
			ImmutableList.Builder<IndexInfo> b = ImmutableList.builder();
			for (IndexInfo ii : ti.indexInfo) {
				Btree btree = idxs.get(ii.columns);
				if (btree == null)
					b.add(ii);
				else
					b.add(new IndexInfo(ii.columns, btree.info()));
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
