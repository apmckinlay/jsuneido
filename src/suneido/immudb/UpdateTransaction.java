/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;
import java.util.Map;

import suneido.immudb.IndexedData.Mode;
import suneido.intfc.database.HistoryIterator;
import suneido.intfc.database.IndexIter;

import com.google.common.collect.ImmutableList;

/**
 * Transactions must be thread contained.
 * They take a "snapshot" of the database state at the start
 * and then update the database state when they commit.
 * Storage is only written during commit.
 * Commit is single-threaded.
 */
class UpdateTransaction extends ReadTransaction {
	protected Tables newSchema;
	protected final UpdateDbInfo dbinfo;
	protected boolean locked = false;
//	private final long asof;
//	private volatile long commitTime = Long.MAX_VALUE;

	/** for Database.updateTran */
	UpdateTransaction(int num, Database db) {
		super(num, db);
		this.dbinfo = new UpdateDbInfo(stor, db.dbinfo);
		newSchema = schema;
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

	@Override
	protected ReadDbInfo dbinfo() {
		return dbinfo;
	}

	/** for Bootstrap and TableBuilder */
	Btree addIndex(int tblnum, int... indexColumns) {
		assert locked;
		Btree btree = new Btree(tran);
		indexes.put(tblnum, new ColNums(indexColumns), btree);
		return btree;
	}

	/** for TableBuilder */
	void addSchemaTable(Table table) {
		assert locked;
		newSchema = newSchema.with(table);
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
		dbinfo.updateRowInfo(tblnum, 1, r.bufSize());
	}

	void updateRecord(int tblnum, Record from, Record to) {
		assert locked;
		indexedData(tblnum).update(from, to);
		dbinfo.updateRowInfo(tblnum, 0, to.bufSize() - from.bufSize());
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

	boolean isCommitted() {
		// TODO isCommitted
		// return commitTime != Long.MAX_VALUE;
		return false;
	}

	boolean committedBefore(UpdateTransaction tran) {
		// TODO committedBefore
		// return commitTime < tran.asof;
		return false;
	}

	@Override
	public void abortIfNotComplete() {
		abortIfNotComplete("aborted");
	}

	void abortIfNotComplete(String conflict) {
		if (locked)
			abort();
	}

	@Override
	public void abort() {
		assert locked;
		unlock();
	}

	// TODO if exception during commit, rollback storage
	@Override
	public String complete() {
		assert locked;
		try {
			synchronized(db.commitLock) {
				tran.startStore();
				DataRecords.store(tran);
				Btree.store(tran);

				updateOurDbinfo();
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
		return "";
	}

	private int updateRedirs() {
		tran.mergeRedirs(db.redirs);
		int redirsAdr = tran.storeRedirs();
		db.redirs = tran.redirs().redirs();
		return redirsAdr;
	}

	private void updateOurDbinfo() {
		for (int tblnum : indexes.rowKeySet()) {
			TableInfo ti = dbinfo.get(tblnum);
			Map<ColNums,Btree> idxs = indexes.row(tblnum);
			ImmutableList.Builder<IndexInfo> b = ImmutableList.builder();
			for (IndexInfo ii : ti.indexInfo) {
				Btree btree = idxs.get(new ColNums(ii.columns));
				b.add((btree == null)
						? ii : new IndexInfo(ii.columns, btree.info()));
			}
			ti = new TableInfo(tblnum, ti.nextfield, ti.nrows(), ti.totalsize(),
					b.build());
			dbinfo.add(ti);
		}
	}

	private void updateDatabaseDbInfo() {
		dbinfo.merge(originalDbinfo(), db.dbinfo);
		db.dbinfo = dbinfo.dbinfo();
	}

	static final int INT_SIZE = 4;

	private void store(int dbinfo, int redirs) {
		ByteBuffer buf = stor.buffer(stor.alloc(2 * INT_SIZE));
		buf.putInt(dbinfo);
		buf.putInt(redirs);
	}

	private void updateSchema() {
		if (newSchema == schema)
			return; // no schema changes in this transaction

		if (db.schema != schema)
			throw schemaConflict;

		db.schema = newSchema;
	}

	private static final Conflict schemaConflict =
			new Conflict("concurrent schema modification");

	static class Conflict extends RuntimeException {
		private static final long serialVersionUID = 1L;

		Conflict(String explanation) {
			super("transaction conflict: " + explanation);
		}
	}

	@Override
	public boolean isReadonly() {
		return false;
	}

	@Override
	public boolean isReadWrite() {
		return true;
	}

	@Override
	public boolean isEnded() {
		return ! locked;
	}

	long asof() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String conflict() {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public void addRecord(String table, suneido.intfc.database.Record r) {
		// TODO Auto-generated method stub

	}

	@Override
	public int updateRecord(int recadr, suneido.intfc.database.Record rec) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int updateRecord(int tblnum, suneido.intfc.database.Record oldrec,
			suneido.intfc.database.Record newrec) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void removeRecord(int off) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeRecord(int tblnum, suneido.intfc.database.Record rec) {
		assert locked;
		indexedData(tblnum).remove((Record) rec);
		dbinfo.updateRowInfo(tblnum, -1, -rec.bufSize());
	}

	@Override
	public void callTrigger(suneido.intfc.database.Table table,
			suneido.intfc.database.Record oldrec,
			suneido.intfc.database.Record newrec) {
		// TODO Auto-generated method stub

	}

	@Override
	public Record fromRef(Object ref) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HistoryIterator historyIterator(int tblnum) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IndexIter iter(int tblnum, String columns) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IndexIter iter(int tblnum, String columns,
			suneido.intfc.database.Record org, suneido.intfc.database.Record end) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IndexIter iter(int tblnum, String columns, IndexIter iter) {
		// TODO Auto-generated method stub
		return null;
	}

}
