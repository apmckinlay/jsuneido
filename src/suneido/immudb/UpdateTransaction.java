/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;

import suneido.immudb.Btree.Iter;
import suneido.immudb.IndexedData.Mode;
import suneido.util.ThreadConfined;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;

/**
 * Transactions must be thread confined.
 * They take a "snapshot" of the database state at the start
 * and then update the database state when they commit.
 * Storage is only written during commit.
 * Commit is single-threaded.
 */
@ThreadConfined
class UpdateTransaction extends ReadTransaction {
	protected final UpdateDbInfo udbinfo;
	protected Tables newSchema;
	protected boolean locked = false;
	private final long asof;
	private volatile long commitTime = Long.MAX_VALUE;
	private String conflict = null;
	private final Transactions trans;
	private volatile boolean inConflict = false;
	private volatile boolean outConflict = false;
	private boolean onlyReads = false; // TODO

	UpdateTransaction(int num, Database db) {
		super(num, db);
		udbinfo = new UpdateDbInfo(stor, db.getDbinfo());
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
		trans.addFinal(this);
		trans.remove(this);
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
		indexes.put(tblnum, new ColNums(indexColumns), btree);
		return btree;
	}

	@Override
	public void addRecord(String table, suneido.intfc.database.Record rec) {
		addRecord(getTable(table).num, (Record) rec);
	}

	void addRecord(int tblnum, Record rec) {
		assert locked;
		rec.tblnum = tblnum;
		indexedData(tblnum).add(rec);
		udbinfo.updateRowInfo(tblnum, 1, rec.bufSize());
	}

	@Override
	public int updateRecord(int fromadr, suneido.intfc.database.Record to) {
		Record from = new Record(stor, fromadr);
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
		assert locked;
		to.tblnum = tblnum;
		indexedData(tblnum).update(from, to);
		udbinfo.updateRowInfo(tblnum, 0, to.bufSize() - from.bufSize());
	}

	void updateAll(int tblnum, int[] colNums, Record oldkey, Record newkey) {
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
		Record rec = new Record(stor, adr);
		removeRecord(rec.tblnum, rec);
	}

	@Override
	public void removeRecord(int tblnum, suneido.intfc.database.Record rec) {
		assert locked;
		indexedData(tblnum).remove((Record) rec);
		udbinfo.updateRowInfo(tblnum, -1, -rec.bufSize());
	}

	void removeAll(int tblnum, int[] colNums, Record key) {
		Iter iter = getIndex(tblnum, colNums).iterator(key);
		for (iter.next(); ! iter.eof(); iter.next())
			removeRecord(iter.keyadr());
	}

	//PERF cache?
	private IndexedData indexedData(int tblnum) {
		IndexedData id = new IndexedData(this);
		Table table = getTable(tblnum);
		if (table == null) {
			int[] indexColumns = Bootstrap.indexColumns[tblnum];
			Btree btree = getIndex(tblnum, indexColumns);
			id.index(btree, Mode.KEY, indexColumns, null, null);
		} else {
			for (Index index : getTable(tblnum).indexes) {
				Btree btree = getIndex(tblnum, index.colNums);
				id.index(btree, index.mode(), index.colNums,
						index.fksrc, schema.getFkdsts(table.name,
								table.numsToNames(index.colNums)));
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
		assert locked;
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
				DataRecords.store(tran);
				Btree.store(tran);

				updateOurDbinfo();
				mergeDatabaseDbInfo();

				mergeRedirs();
				int redirsAdr = updateRedirs();
				int dbinfoAdr = udbinfo.store();
				store(dbinfoAdr, redirsAdr);
				tran.endStore();

				db.setDbinfo(udbinfo.dbinfo());
				updateSchema();

				commitTime = trans.clock();
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

	static final int INT_SIZE = 4;

	protected void store(int dbinfo, int redirs) {
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
		return asof;
	}

	@Override
	public String conflict() {
		return conflict ;
	}

	private void notEnded() {
		if (isEnded())
			throw new RuntimeException("cannot use ended transaction");
	}

	private boolean isActive() {
		return ! isEnded();
	}

	private void abortThrow(String conflict) {
		this.conflict = conflict;
		abort();
		throw new RuntimeException("transaction " + conflict);
	}

	private boolean committedAfter(UpdateTransaction t) {
		return isCommitted() && commitTime > t.asof;
	}

	@Override
	public void readLock(int adr) {
		notEnded();
		UpdateTransaction writer = trans.readLock(this, adr);
		if (writer != null) {
			if (this.inConflict || writer.outConflict)
				abortThrow("conflict (read-write) with " + writer);
			writer.inConflict = true;
			this.outConflict = true;
		}

		Set<UpdateTransaction> writes = trans.writes(adr);
		for (UpdateTransaction w : writes) {
			if (w == this || w.commitTime < asof)
				continue;
			if (w.outConflict)
				abortThrow("conflict (read-write) with " + w);
			this.outConflict = true;
		}
		for (UpdateTransaction w : writes)
			w.inConflict = true;
	}

	@Override
	public void writeLock(int adr) {
		notEnded();
		onlyReads = false;
		Set<UpdateTransaction> readers = trans.writeLock(this, adr);
		if (readers == null)
			abortThrow("conflict (write-write)");
		for (UpdateTransaction reader : readers)
			if (reader.isActive() || reader.committedAfter(this)) {
				if (reader.inConflict || this.outConflict)
					abortThrow("conflict (write-read) with " + reader);
				this.inConflict = true;
			}
		for (UpdateTransaction reader : readers)
			reader.outConflict = true;
	}

	@Override
	public String toString() {
		return "ut" + num;
	}

}
