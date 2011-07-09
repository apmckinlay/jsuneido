/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;
import java.util.Map;

import suneido.immudb.IndexedData.Mode;
import suneido.immudb.schema.*;

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
	protected boolean locked = false;

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
		locked = true;
	}

	protected void unlock() {
		db.exclusiveLock.readLock().unlock();
		locked = false;
	}

	/** for Bootstrap and TableBuilder */
	Btree addIndex(int tblnum, String indexColumns) {
		assert locked;
		Btree btree = new Btree(tran);
		indexes.put(tblnum, indexColumns, btree);
		return btree;
	}

	/** for TableBuilder */
	void addSchemaTable(Table table) {
		assert locked;
		schema = schema.with(table);
	}

	/** for TableBuilder */
	void addTableInfo(TableInfo ti) {
		assert locked;
		dbinfo.add(ti);
	}

	/** for TableBuilder */
	void addRecord(int tblnum, Record r) {
		assert locked;
		indexedData(tblnum).add(r);
		dbinfo.updateRowInfo(tblnum, 1, r.length());
	}

	void removeRecord(int tblnum, Record r) {
		assert locked;
		indexedData(tblnum).remove(r);
		dbinfo.updateRowInfo(tblnum, -1, -r.length());
	}

	void updateRecord(int tblnum, Record from, Record to) {
		assert locked;
		indexedData(tblnum).update(from, to);
		dbinfo.updateRowInfo(tblnum, 0, to.length() - from.length());
	}

	private IndexedData indexedData(int tblnum) {
		IndexedData id = new IndexedData(tran);
		Table table = getTable(tblnum);
		if (table == null) {
			int[] indexColumns = bootstrap[tblnum - 1];
			Btree btree = getIndex(tblnum, indexColumns);
			id.index(btree, Mode.KEY, indexColumns);
		} else {
			for (Index index : getTable(tblnum).indexes) {
				Btree btree = getIndex(tblnum, index.colNums);
				id.index(btree, index.mode(), index.colNums);
			}
		}
		return id;
	}
	private static final int[][] bootstrap = new int[][] {
		new int[] { 0 }, new int[] { 0,1 }, new int[] { 0,1 }
	};

	// commit -----------------------------------------------------------------

	public void abort() {
		assert locked;
		unlock();
	}

	public void abortIfNotCommitted() {
		if (locked)
			abort();
	}

	// TODO if exception during commit, rollback storage
	public void commit() {
		assert locked;
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

	private int updateRedirs() {
		tran.mergeRedirs(db.redirs);
		int redirsAdr = tran.storeRedirs();
		db.redirs = tran.redirs().redirs();
		return redirsAdr;
	}

	private void updateOurDbInfo() {
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

	private void updateDatabaseDbInfo() {
		dbinfo.merge(originalDbInfo, db.dbinfo);
		db.dbinfo = dbinfo.dbinfo();
	}

	static final int INT_SIZE = 4;

	private void store(int dbinfo, int redirs) {
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
