/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.Map;

import suneido.SuException;
import suneido.immudb.Bootstrap.TN;
import suneido.immudb.IndexedData.Mode;
import suneido.intfc.database.IndexIter;
import suneido.util.ThreadConfined;

import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

/**
 * Transactions must be thread confined.
 * They take a "snapshot" of the database state at the start
 * and then update the database state when they commit.
 * Storage is only written during commit.
 * Commit is single-threaded.
 */
@ThreadConfined
class UpdateTransaction2 extends ReadTransaction2 {
	protected final UpdateDbInfo udbinfo;
	protected Tables newSchema;
	protected boolean locked = false;
	private final long asof;
	private volatile long commitTime = Long.MAX_VALUE;
	private String conflict = null;
	private final boolean onlyReads = false;
	private final Map<Index,TransactionReads> reads = Maps.newHashMap();

	UpdateTransaction2(int num, Database2 db) {
		super(num, db);
		udbinfo = new UpdateDbInfo(stor, dbstate.dbinfo);
		newSchema = schema;
		asof = db.trans.clock();
		lock(db);
	}

	protected void lock(Database2 db) {
		assert ! locked;
		db.exclusiveLock.readLock().lock();
		locked = true;
	}

	protected void unlock() {
		assert locked;
		db.exclusiveLock.readLock().unlock();
		locked = false;
	}

	@Override
	public Table getTable(String tableName) {
		return newSchema.get(tableName);
	}

	@Override
	public Table getTable(int tblnum) {
		return newSchema.get(tblnum);
	}

	@Override
	TableInfo getTableInfo(int tblnum) {
		return udbinfo.get(tblnum);
	}

	/** for Bootstrap and TableBuilder */
	Btree addIndex(int tblnum, int... indexColumns) {
		assert locked;
		Btree btree = new Btree(tran, this);
		indexes.put(index(tblnum, indexColumns), btree);
		return btree;
	}

	@Override
	public void addRecord(String table, suneido.intfc.database.Record rec) {
		addRecord(getTable(table).num, (Record) rec);
	}

	void addRecord(int tblnum, Record rec) {
		verifyNotSystemTable(tblnum, "output");
		assert locked;
		rec.tblnum = tblnum;
		indexedData(tblnum).add(rec);
		callTrigger(getTable(tblnum), null, rec);
		udbinfo.updateRowInfo(tblnum, 1, rec.bufSize());
	}

	@Override
	public int updateRecord(int fromadr, suneido.intfc.database.Record to) {
		if (fromadr == 1)
			throw new SuException("can't update the same record multiple times");
		Record from = tran.getrec(fromadr);
		updateRecord(from.tblnum, from, (Record) to);
		return 1; //TODO don't know record address till commit
	}

	@Override
	public int updateRecord(int tblnum,
			suneido.intfc.database.Record from,
			suneido.intfc.database.Record to) {
		updateRecord(tblnum, (Record) from, (Record) to);
		return 1; //TODO don't know record address till commit
	}

	void updateRecord(int tblnum, Record from, Record to) {
		verifyNotSystemTable(tblnum, "update");
		assert locked;
		to.tblnum = tblnum;
		indexedData(tblnum).update(from, to);
		callTrigger(ck_getTable(tblnum), from, to);
		udbinfo.updateRowInfo(tblnum, 0, to.bufSize() - from.bufSize());
	}

	// used by foreign key cascade
	void updateAll(int tblnum, int[] colNums, Record oldkey, Record newkey) {
		IndexIter iter = getIndex(tblnum, colNums).iterator(oldkey);
		for (iter.next(); ! iter.eof(); iter.next()) {
			Record oldrec = input(iter.keyadr());
			RecordBuilder rb = new RecordBuilder();
			for (int i = 0; i < oldrec.size(); ++i) {
				int j = Ints.indexOf(colNums, i);
				rb.add(j == -1 ? oldrec.get(i) : newkey.get(j));
			}
			updateRecord(tblnum, oldrec, rb.build());
		}
	}

	@Override
	public void removeRecord(int adr) {
		Record rec = tran.getrec(adr);
		removeRecord(rec.tblnum, rec);
	}

	@Override
	public void removeRecord(int tblnum, suneido.intfc.database.Record rec) {
		verifyNotSystemTable(tblnum, "delete");
		assert locked;
		indexedData(tblnum).remove((Record) rec);
		callTrigger(ck_getTable(tblnum), rec, null);
		udbinfo.updateRowInfo(tblnum, -1, -rec.bufSize());
	}

	// used by foreign key cascade
	void removeAll(int tblnum, int[] colNums, Record key) {
		IndexIter iter = getIndex(tblnum, colNums).iterator(key);
		for (iter.next(); ! iter.eof(); iter.next())
			removeRecord(iter.keyadr());
	}

	void verifyNotSystemTable(int tblnum, String what) {
		if (tblnum <= TN.VIEWS)
			throw new SuException("can't " + what + " system table");
	}

	//PERF cache?
	private IndexedData2 indexedData(int tblnum) {
		IndexedData2 id = new IndexedData2(this);
		Table table = getTable(tblnum);
		if (table == null) {
			int[] indexColumns = Bootstrap.indexColumns[tblnum];
			TranIndex btree = getIndex(tblnum, indexColumns);
			id.index(btree, Mode.KEY, indexColumns, "", null, null);
		} else {
			for (Index index : getTable(tblnum).indexes) {
				TranIndex btree = getIndex(tblnum, index.colNums);
				String colNames = table.numsToNames(index.colNums);
				id.index(btree, index.mode(), index.colNums, colNames,
						index.fksrc, schema.getFkdsts(table.name, colNames));
			}
		}
		return id;
	}

	// commit -----------------------------------------------------------------

	boolean isCommitted() {
		return commitTime != Long.MAX_VALUE;
	}

	boolean committedBefore(UpdateTransaction2 tran) {
		return commitTime < tran.asof;
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
		trans.abort(this);
		unlock();
	}

	@Override
	public boolean isAborted() {
		return isEnded() && !isCommitted();
	}

	// TODO if exception during commit, nullify by writing same trailer as last commit
	@Override
	public String complete() {
		if (isAborted())
			return conflict;
		assert locked;
		if (onlyReads) {
			abort();
			return null;
		}
		try {
			synchronized(db.commitLock) {
				tran.startStore();
				try {
					DataRecords.store(tran);
					Btree.store(tran);

					int dbinfoAdr = udbinfo.store();
//					store(dbinfoAdr, redirsAdr); //BUG if exception, won't get done
				} finally {
					tran.endStore();
				}

//				db.setDbinfo(udbinfo.dbinfo());
				updateSchema();

				commitTime = trans.clock();
				trans.commit(this);
			}
		} catch(Conflict c) {
			conflict = c.toString();
			return conflict;
		} finally {
			unlock();
		}
		return null;
	}

	static final int INT_SIZE = 4;

	protected void store(int dbinfo, int redirs) {
		ByteBuffer buf = stor.buffer(stor.alloc(2 * INT_SIZE));
		buf.putInt(dbinfo);
		buf.putInt(redirs);
	}

	private void updateSchema() {
		if (newSchema == schema)
			return; // no schema changes in this transaction

//		if (db.schema != schema)
//			throw schemaConflict;
//
//		db.schema = newSchema;
	}

	private static final Conflict schemaConflict =
			new Conflict("concurrent schema modification");

	static class Conflict extends SuException {
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
		return asof;
	}

	long commitTime() {
		return commitTime;
	}

	@Override
	public String conflict() {
		return conflict ;
	}

	private void notEnded() {
		if (isEnded())
			throw new SuException("cannot use ended transaction");
	}

	private boolean isActive() {
		return ! isEnded();
	}

	void abortThrow(String conflict) {
		this.conflict = conflict;
		abort();
		throw new SuException("transaction " + conflict);
	}

	private boolean committedAfter(UpdateTransaction2 t) {
		return isCommitted() && commitTime > t.asof;
	}

	// need for PriorityQueue's in Transactions
	static final Comparator<UpdateTransaction2> byCommit = new Comparator<UpdateTransaction2>() {
		@Override
		public int compare(UpdateTransaction2 t1, UpdateTransaction2 t2) {
			return Longs.compare(t1.commitTime, t2.commitTime);
		}
	};
	static final Comparator<UpdateTransaction2> byAsof = new Comparator<UpdateTransaction2>() {
		@Override
		public int compare(UpdateTransaction2 t1, UpdateTransaction2 t2) {
			return Longs.compare(t1.asof, t2.asof);
		}
	};

	@Override
	public String toString() {
		return "ut" + num;
	}

}
