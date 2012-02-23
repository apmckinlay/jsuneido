/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import suneido.SuException;
import suneido.Suneido;
import suneido.immudb.Bootstrap.TN;
import suneido.immudb.Btree.Iter;
import suneido.immudb.IndexedData.Mode;
import suneido.util.ThreadConfined;

import com.google.common.collect.ImmutableList;
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
class UpdateTransaction extends ReadTransaction implements ImmuUpdateTran {
	protected final UpdateDbInfo udbinfo;
	protected Tables newSchema;
	protected boolean locked = false;
	private final long asof;
	private volatile long commitTime = Long.MAX_VALUE;
	private String conflict = null;
	private final Transactions trans;
	private volatile boolean inConflict = false;
	private volatile boolean outConflict = false;
	private boolean onlyReads = false;

	UpdateTransaction(int num, Database db) {
		super(num, db);
		udbinfo = new UpdateDbInfo(db.getDbinfo());
		newSchema = schema;
		asof = db.trans.clock();
		trans = db.trans;
		lock(db);
	}

	protected void lock(Database db) {
		db.exclusiveLock.readLock().lock();
		trans.add(this);
		locked = true;
	}

	protected void unlock() {
		assert(locked);
		try {
			trans.remove(this);
		} finally {
			db.exclusiveLock.readLock().unlock();
			locked = false;
		}
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
	public TableInfo getTableInfo(int tblnum) {
		return udbinfo.get(tblnum);
	}

	@Override
	public void addRecord(String table, suneido.intfc.database.Record rec) {
		addRecord(getTable(table).num, (Record) rec);
	}

	@Override
	public void addRecord(int tblnum, Record rec) {
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
		return 1; // don't know record address till commit
	}

	@Override
	public int updateRecord(int tblnum,
			suneido.intfc.database.Record from,
			suneido.intfc.database.Record to) {
		updateRecord(tblnum, (Record) from, (Record) to);
		return 1; // don't know record address till commit
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
	@Override
	public void updateAll(int tblnum, int[] colNums, Record oldkey, Record newkey) {
		Iter iter = getIndex(tblnum, colNums).iterator(oldkey);
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
	@Override
	public void removeAll(int tblnum, int[] colNums, Record key) {
		Iter iter = getIndex(tblnum, colNums).iterator(key);
		for (iter.next(); ! iter.eof(); iter.next())
			removeRecord(iter.keyadr());
	}

	void verifyNotSystemTable(int tblnum, String what) {
		if (tblnum <= TN.VIEWS)
			throw new SuException("can't " + what + " system table");
	}

	//PERF cache?
	private IndexedData indexedData(int tblnum) {
		IndexedData id = new IndexedData(this);
		Table table = getTable(tblnum);
		if (table == null) {
			int[] indexColumns = Bootstrap.indexColumns[tblnum];
			Btree btree = getIndex(tblnum, indexColumns);
			id.index(btree, Mode.KEY, indexColumns, "", null, null);
		} else {
			for (Index index : getTable(tblnum).indexes) {
				Btree btree = getIndex(tblnum, index.colNums);
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

	boolean committedBefore(UpdateTransaction tran) {
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

					updateOurDbinfo();
					mergeDatabaseDbInfo();

					mergeRedirs();
					int redirsAdr = updateRedirs();
					int dbinfoAdr = udbinfo.store(stor);
					store(dbinfoAdr, redirsAdr); //BUG if exception, won't get done
				} finally {
					tran.endStore();
				}

				db.setDbinfo(udbinfo.dbinfo());
				updateSchema();

				commitTime = trans.clock();
				trans.addFinal(this);
			}
		} catch(Conflict c) {
			conflict = c.toString();
			return conflict;
		} finally {
			unlock();
		}
		return null;
	}

	private void updateOurDbinfo() {
		for (int tblnum : indexes.rowKeySet()) {
			TableInfo ti = udbinfo.get(tblnum);
			Map<ColNums,Btree> idxs = indexes.row(tblnum);
			ImmutableList.Builder<IndexInfo> b = ImmutableList.builder();
			for (IndexInfo ii : ti.indexInfo) {
				Btree btree = idxs.get(new ColNums(ii.columns));
				b.add((btree == null)
						? ii : new IndexInfo(ii.columns, btree.info()));
			}
			ti = new TableInfo(tblnum, ti.nextfield, ti.nrows(), ti.totalsize(),
					b.build());
			udbinfo.add(ti);
		}
	}

	protected void mergeDatabaseDbInfo() {
		udbinfo.merge(rdbinfo.dbinfo, db.getDbinfo());
	}

	protected void mergeRedirs() {
		tran.mergeRedirs(db.getRedirs());
	}

	private int updateRedirs() {
		int redirsAdr = tran.storeRedirs();
		db.setRedirs(tran.redirs().redirs());
		return redirsAdr;
	}

	protected void store(int dbinfo, int redirs) {
		ByteBuffer buf = stor.buffer(stor.alloc(2 * Ints.BYTES));
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

	@Override
	public void abortThrow(String conflict) {
		this.conflict = conflict;
		abort();
		throw new SuException("transaction " + conflict);
	}

	private boolean committedAfter(UpdateTransaction t) {
		return isCommitted() && commitTime > t.asof;
	}

	@Override
	public void readLock(int adr) {
		notEnded();
		if (Suneido.cmdlineoptions.snapshotIsolation)
			return;

		UpdateTransaction writer = trans.readLock(this, adr);
		if (writer != null) {
			if (this.inConflict || writer.outConflict)
				abortThrow("conflict (read-write) " + this + " with " + writer);
			writer.inConflict = true;
			this.outConflict = true;
		}

		Set<UpdateTransaction> writes = trans.writes(adr);
		for (UpdateTransaction w : writes)
			if (w != this && w.committedAfter(this)) {
				if (this.inConflict || w.outConflict)
					abortThrow("conflict (read-write) " + this + " with " + w);
				this.outConflict = true;
			}

		for (UpdateTransaction w : writes)
			w.inConflict = true;
	}

	@Override
	public void writeLock(int adr) {
		if (IntRefs.isIntRef(adr))
			return;
		notEnded();
		onlyReads = false;
		Set<UpdateTransaction> readers = trans.writeLock(this, adr);
		for (UpdateTransaction reader : readers)
			if (reader.isActive() || reader.committedAfter(this)) {
				if (reader.inConflict || this.outConflict)
					abortThrow("conflict (write-read) with " + reader);
				this.inConflict = true;
			}
		for (UpdateTransaction reader : readers)
			reader.outConflict = true;
	}

	// need for PriorityQueue's in Transactions
	static final Comparator<UpdateTransaction> byCommit = new Comparator<UpdateTransaction>() {
		@Override
		public int compare(UpdateTransaction t1, UpdateTransaction t2) {
			return Longs.compare(t1.commitTime, t2.commitTime);
		}
	};
	static final Comparator<UpdateTransaction> byAsof = new Comparator<UpdateTransaction>() {
		@Override
		public int compare(UpdateTransaction t1, UpdateTransaction t2) {
			return Longs.compare(t1.asof, t2.asof);
		}
	};

	@Override
	public String toString() {
		return "ut" + num;
	}

}
