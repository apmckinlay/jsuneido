/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;
import java.util.Map;

import suneido.database.immudb.schema.Table;
import suneido.database.immudb.schema.Tables;

import com.google.common.collect.ImmutableList;

/**
 * Transactions must be thread contained.
 * They take a "snapshot" of the database state at the start
 * and then update the database state when they commit.
 * Storage is only written during commit.
 * Commit is single-threaded.
 */
public class UpdateTransaction extends ReadTransaction {
	private final Database db;
	private final Tables original_schema;

	/** for Database.updateTran */
	UpdateTransaction(Database db) {
		super(db);
		this.db = db;
		original_schema = db.schema;
	}

	/** for Bootstrap and TableBuilder */
	void addIndex(int tblnum, String indexColumns) {
		indexes.put(tblnum, indexColumns, new Btree(tran));
	}

	/** for TableBuilder */
	void addSchemaTable(Table table) {
		schema = schema.with(table);
	}

	/** for TableBuilder */
	void addTableInfo(TableInfo ti) {
		dbinfo.add(ti);
	}

	/** for TableBuilder */
	void addRecord(Record r, int tblnum, int... indexColumns) {
		Btree btree = getIndex(tblnum, indexColumns);
		IndexedData id = new IndexedData().index(btree, indexColumns);
		id.add(tran, r);
	}

	public void commit() {
		synchronized(db.commitLock) {
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
	}

	private void updateDbInfo() {
		for (int tblnum : indexes.rowKeySet()) {
			TableInfo ti = dbinfo.get(tblnum);
			Map<String,Btree> idxs = indexes.row(tblnum);
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

		if (db.schema != original_schema)
			throw conflict;

		db.schema = schema;
	}

	private static final Conflict conflict = new Conflict();

	public static class Conflict extends RuntimeException {
		private static final long serialVersionUID = 1L;

		Conflict() {
			super("transaction conflict: concurrent schema modification");
		}
	}

}
