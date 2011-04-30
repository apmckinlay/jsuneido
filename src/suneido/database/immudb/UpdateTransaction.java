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
	protected final Database db;
	private final Tables originalSchema;
	private final DbHashTrie originalDbInfo;

	/** for Database.updateTran */
	UpdateTransaction(Database db) {
		super(db);
		this.db = db;
		originalSchema = db.schema;
		originalDbInfo = db.dbinfo;
		lock(db);
	}

	protected void lock(Database db) {
		db.exclusiveLock.readLock().lock();
	}

	protected void unlock() {
		db.exclusiveLock.readLock().unlock();
	}

	/** for Bootstrap and TableBuilder */
	Btree addIndex(int tblnum, String indexColumns) {
		Btree btree = new Btree(tran);
		indexes.put(tblnum, indexColumns, btree);
		return btree;
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
		dbinfo.addrow(tblnum, r.length());
	}

	public void addRecord(String tablename, Record rec) {
		// TODO Auto-generated method stub

	}

	// commit -------------------------------------------------------

	public void abort() {
		unlock();
	}

	// TODO if exception during commit, rollback storage
	public void commit() {
		try {
			synchronized(db.commitLock) {
				tran.startStore();
				DataRecords.store(tran);
				Btree.store(tran);

				updateOurDbInfo();
				updateDatabaseDbInfo();

				int redirsAdr = updateRedirs();
				int dbinfoAdr = dbinfo.store();
				store(dbinfoAdr, redirsAdr);
				tran.endStore();

				updateSchema();
			}
		} finally {
			unlock();
		}
	}

	protected int updateRedirs() {
		tran.mergeRedirs(db.redirs);
		int redirsAdr = tran.storeRedirs();
		db.redirs = tran.redirs().redirs();
		return redirsAdr;
	}

	protected void updateOurDbInfo() {
		for (int tblnum : indexes.rowKeySet()) {
			TableInfo ti = dbinfo.get(tblnum);
			Map<String,Btree> idxs = indexes.row(tblnum);
			ImmutableList.Builder<IndexInfo> b = ImmutableList.builder();
			for (IndexInfo ii : ti.indexInfo) {
				Btree btree = idxs.get(ii.columns);
				b.add((btree == null)
						? ii : new IndexInfo(ii.columns, btree.info()));
			}
			ti = new TableInfo(tblnum, ti.nextfield, ti.nrows(), ti.totalsize(),
					b.build());
			dbinfo.add(ti);
		}
	}

	protected void updateDatabaseDbInfo() {
		dbinfo.merge(originalDbInfo, db.dbinfo);
		db.dbinfo = dbinfo.dbinfo();
	}

	static final int INT_SIZE = 4;

	protected void store(int dbinfo, int redirs) {
		ByteBuffer buf = stor.buffer(stor.alloc(2 * INT_SIZE));
		buf.putInt(dbinfo);
		buf.putInt(redirs);
	}

	private void updateSchema() {
		if (schema == originalSchema)
			return; // no schema changes in this transaction

		if (db.schema != originalSchema)
			throw schemaConflict;

		db.schema = schema;
	}

	private static final Conflict schemaConflict =
			new Conflict("concurrent schema modification");

	public static class Conflict extends RuntimeException {
		private static final long serialVersionUID = 1L;

		Conflict(String explanation) {
			super("transaction conflict: " + explanation);
		}
	}

}
