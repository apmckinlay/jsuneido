/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Shorts;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.hash.TIntHashSet;
import suneido.immudb.TranIndex.Iter;
import suneido.intfc.database.IndexIter;
import suneido.util.Errlog;
import suneido.util.ThreadConfined;

/**
 * Transactions are thread confined
 * (except for Transactions.limitOutstanding)
 * They take a "snapshot" of the database state at the start
 * and then update the database state when they commit.
 * Storage is only written during commit.
 * Commit is single-threaded.
 * <p>
 * Need to synchronize abort and commit because transactions may be aborted
 * from Transactions.limitOutstanding.
 */
@ThreadConfined
class UpdateTransaction extends ReadWriteTransaction {
	private final long asof;
	private volatile long commitTime = Long.MAX_VALUE;
	private final Map<Index,TransactionReads> reads = Maps.newHashMap();
	/** updated by IndexedData, used by OverlayIndex */
	protected final TIntHashSet deletes = new TIntHashSet();
	/** used after commit by other transactions checking for conflicts */
	private final TIntHashSet inserts = new TIntHashSet();
	/** needs to be ordered tree for ReadWriteTransaction updateDbInfo */
	protected final TreeMap<Index,TranIndex> updatedIndexes = Maps.newTreeMap();
	private final TIntArrayList actions = new TIntArrayList();
	private int writeCount = 0;
	static int MAX_WRITES_PER_TRANSACTION = 10000;
	final static int WARN_WRITES_PER_TRANSACTION = 5000;
	final static int WARN_UPDATE_TRAN_DURATION_SEC = 5;
	protected static final short UPDATE = (short) 0;
	protected static final short REMOVE = (short) -1;
	protected static final short END = (short) -2;
	/** Used by {@link Transactions} limitOutstanding */
	final Stopwatch stopwatch = Stopwatch.createStarted();

	UpdateTransaction(int num, Database db) {
		super(num, db);
		asof = db.trans.clock();
		trans.addUpdateTran(this); // must be after setting asof
	}

	@Override
	protected IndexedData indexedData2(int tblnum) {
		return super.indexedData2(tblnum).setDeletes(deletes);
	}

	@Override
	protected TranIndex getIndex(IndexInfo info) {
		assert info != null : "missing IndexInfo";
		return new OverlayIndex(new Btree(tran, info), new Btree(tran), deletes);
	}

	@Override
	public int addRecord(int tblnum, DataRecord rec) {
		int adr = super.addRecord(tblnum, rec);
		addAction(adr);
		++writeCount;
		return adr;
	}

	@Override
	protected int updateRecord2(int tblnum, DataRecord from, DataRecord to,
			Blocking blocking) {
		to.address(tran.refToInt(to));
		int fromadr = super.updateRecord2(tblnum, from, to, blocking);
		addAction(UPDATE);
		actions.add(fromadr);
		actions.add(to.address());
		++writeCount;
		return fromadr;
	}

	@Override
	public int removeRecord(int tblnum, Record rec) {
		int adr = super.removeRecord(tblnum, rec);
		addAction(adr);
		++writeCount;
		return adr;
	}

	private void addAction(int action) {
		if (writeCount > MAX_WRITES_PER_TRANSACTION)
			abortThrow("too many writes (output, update, or delete) in one transaction");
		actions.add(action);
	}

	// used by foreign key cascade
	@Override
	void updateAll(int tblnum, int[] colNums, Record oldkey, Record newkey) {
		Index index = index(tblnum, colNums);
		Iter iter = getIndex(index).iterator(oldkey);
		trackReads(index, iter);
		for (iter.next(); ! iter.eof(); iter.next()) {
			Record oldrec = input(iter.keyadr());
			RecordBuilder rb = new RecordBuilder();
			for (int i = 0; i < oldrec.size(); ++i) {
				int j = Ints.indexOf(colNums, i);
				rb.add(j == -1 ? oldrec.get(i) : newkey.get(j));
			}
			updateRecord(tblnum, oldrec, rb.build(), Blocking.NO_BLOCK);
		}
	}

	// used by foreign key cascade
	@Override
	void removeAll(int tblnum, int[] colNums, Record key) {
		Index index = index(tblnum, colNums);
		Iter iter = getIndex(index).iterator(key);
		trackReads(index, iter);
		for (iter.next(); ! iter.eof(); iter.next())
			removeRecord(iter.keyadr());
	}

	@Override
	public IndexIter iter(int tblnum, String columns) {
		Index index = index(tblnum, columns);
		Iter iter = getIndex(index).iterator();
		trackReads(index, iter);
		return iter;
	}

	@Override
	public IndexIter iter(int tblnum, String columns,
			suneido.intfc.database.Record org, suneido.intfc.database.Record end) {
		Index index = index(tblnum, columns);
		Iter iter = getIndex(index).iterator((Record) org, (Record) end);
		trackReads(index, iter);
		return iter;
	}

	@Override
	public IndexIter iter(int tblnum, String columns, IndexIter iter) {
		Index index = index(tblnum, columns);
		Iter iter2 = getIndex(index).iterator(iter);
		trackReads(index, iter2);
		return iter2;
	}

	private void trackReads(Index index, Iter iter) {
		((OverlayIndex.Iter) iter).trackRange(indexRange(index));
	}

	private IndexRange indexRange(Index index) {
		TransactionReads tr = reads.get(index);
		if (tr == null)
			reads.put(index, tr = new TransactionReads(this));
		IndexRange ir = new IndexRange();
		tr.add(ir);
		return ir;
	}

	// -------------------------------------------------------------------------

	@Override
	// just adds synchronized to ReadWriteTransaction.complete
	synchronized public String complete() {
		return super.complete();
	}

	private boolean isCommitted() {
		return commitTime != Long.MAX_VALUE;
	}

	@Override
	public void abortIfNotComplete() {
		abortIfNotComplete("aborted");
	}

	synchronized void abortIfNotComplete(String conflict) {
		if (! ended)
			abort(conflict);
	}

	@Override
	public boolean isAborted() {
		return ended && ! isCommitted();
	}

	@Override
	// just adds synchronized to ReadWriteTransaction.abort
	synchronized public void abort() {
		super.abort();
	}

	// commit ------------------------------------------------------------------

	@Override
	protected void commit() {
		buildReads();
		synchronized(db.commitLock) {
			if (db.state.schema != dbstate.schema)
				throw new Conflict("schema changed");
			checkForConflicts();
			storeData();
			try {
				updateBtrees();
				updateDbInfo();
				finish();
			} catch (Throwable e) {
				tran.abortIncompleteStore();
				throw e;
			}
		}
		if (writeCount > WARN_WRITES_PER_TRANSACTION)
			Errlog.warn("excessive writes (" + writeCount +
					") writes (output/update/delete) in one transaction " + this);
		long secs = stopwatch.elapsed(TimeUnit.SECONDS);
		if (secs > WARN_UPDATE_TRAN_DURATION_SEC &&
				! (this instanceof SchemaTransaction)) {
			String msg = ": long duration update transaction " + this +
								" (" + secs + " seconds)";
			if (secs < Transactions.MAX_UPDATE_TRAN_DURATION_SEC + 2)
				Errlog.warn(msg);
			else
				Errlog.error(msg);
		}
	}

	private void buildReads() {
		for (TransactionReads tr : reads.values())
			tr.build();
	}

	protected void checkForConflicts() {
		// for each overlapping transaction
		Set<UpdateTransaction> overlapping = trans.getOverlapping(asof);
		for (UpdateTransaction t : overlapping) {
			assert t != this;
			TIntIterator iter = t.deletes.iterator();
			while (iter.hasNext()) {
				int del = iter.next();
				// check if it deleted from an index range that we read
				readValidation(del);
				// check if we deleted the same record
				checkForDeleteConflict(del);
			}
			iter = t.inserts.iterator();
			while (iter.hasNext())
				// check if it inserted into an index range that we read
				readValidation(iter.next());
		}
	}

	// COULD make reads a Table<tblnum,index>
	// so you could easily get all the indexes for a tblnum
	private void readValidation(int adr) {
		DataRecord r = input(adr);
		Table table = ck_getTable(r.tblnum());
		List<Index> indexes = table.indexesList();
		for (Index index : indexes) {
			TransactionReads tr = reads.get(index);
			if (tr == null)
				continue;
			Record key = key(r, index.colNums);
			if (tr.contains(key))
				throw new Conflict("read in " + table.name);
		}
	}

	private static Record key(Record r, int[] colNums) {
		return new RecordBuilder().addFields(r, colNums).arrayRec();
	}

	private void checkForDeleteConflict(int del) {
		if (deletes.contains(del))
			throw new Conflict("delete");
	}

	// store data --------------------------------------------------------------

	protected void storeData() {
		tran.startStore();
		startCommit();
		storeActions();
		endCommit(tran.dstor);
	}

	protected void startCommit() {
		ByteBuffer buf = tran.dstor.buffer(tran.dstor.alloc(1));
		buf.put((byte) tranType());
	}

	/** overridden by SchemaTransaction */
	protected char tranType() {
		return 'u';
	}

	private void storeActions() {
		TIntIntHashMap updates = new TIntIntHashMap();
		for (TIntIterator iter = actions.iterator(); iter.hasNext(); ) {
			int act = iter.next();
			if (act == UPDATE) {
				int rem = iter.next();
				int add = iter.next();
				updateAction(updates, rem, add);
			} else if (IntRefs.isIntRef(act)) {
				if (! wasDeleted(act))
					storeAdd(act);
			} else {
				if (! newRecord(act))
					storeRemove(act);
			}
		}
	}

	/** overridden by tests */
	protected void storeAdd(int act) {
		DataRecord rec = (DataRecord) tran.intToRef(act);
		int adr = rec.store(tran.dstor);
		tran.setAdr(act, adr);
		inserts.add(adr);
	}

	/** overridden by tests */
	protected void storeRemove(int act) {
		ByteBuffer buf = tran.dstor.buffer(
				tran.dstor.alloc(Shorts.BYTES + Ints.BYTES));
		buf.putShort(REMOVE);
		buf.putInt(act);
		assert deletes.contains(act);
	}

	private void updateAction(TIntIntHashMap updates, int from, int to) {
		if (updates.contains(from))
			from = updates.get(from);
		if (updatedAgainLater(updates, from, to))
			return;
		else if (newRecord(from)) {
			if (wasDeleted(to))
				return;
			else
				storeAdd(to);
		} else { // from is old
			if (wasDeleted(to))
				storeRemove(from);
			else
				storeUpdate(from, to);
		}
	}

	private boolean updatedAgainLater(TIntIntHashMap updates, int from, int to) {
		if (tran.intToRef(to) != IndexedData.UPDATED)
			return false;
		updates.put(to, from); // so subsequent update can be from first "from"
		return true;
	}

	/** overridden by tests */
	protected void storeUpdate(int from, int to) {
		ByteBuffer buf = tran.dstor.buffer(
				tran.dstor.alloc(Shorts.BYTES + Ints.BYTES));
		buf.putShort(UPDATE);
		buf.putInt(from);
		storeAdd(to);
	}

	private static boolean newRecord(int act) {
		return IntRefs.isIntRef(act);
	}

	private boolean wasDeleted(int act) {
		Object ref = tran.intToRef(act);
		return ref == IndexedData.REMOVED || ref == IndexedData.UPDATED;
	}

	protected static void endCommit(Storage stor) {
		stor.buffer(stor.alloc(Shorts.BYTES)).putShort(END);
	}

	// update btrees -----------------------------------------------------------

	private void updateBtrees() {
		//PERF update in parallel
		for (Entry<Index, TranIndex> e : indexes.entrySet())
			updateBtree(e.getKey(), e.getValue());
	}

	private void updateBtree(Index index, TranIndex idx) {
		Btree global = getLatestIndex(index);
		Btree local;
		boolean updated;
		if (idx instanceof Btree) {
			local = (Btree) idx;
			updated = true;
			if (local.frozen()) {
				// created by TableBuilder
				updatedIndexes.put(index, local);
				return;
			}
		} else {
			OverlayIndex oti = (OverlayIndex) idx;
			updated = ! oti.removedKeys.isEmpty();
			for (BtreeKey key : oti.removedKeys)
				if (! global.remove(key))
					throw new Conflict("missing key");
			local = oti.local();
		}
		Btree.Iter iter = local.iterator();
		for (iter.next(); ! iter.eof(); iter.next()) {
			if (! global.add(translate(iter.cur()), index.isKey, index.unique))
				throw new Conflict("duplicate key");
			updated = true;
		}
		if (updated) {
			global.freeze();
			updatedIndexes.put(index, global);
		}
		assert global.frozen();
	}

	/** overridden by SchemaTransaction */
	protected Btree getLatestIndex(Index index) {
		TableInfo ti = (TableInfo) db.state.dbinfo.get(index.tblnum);
		IndexInfo ii = ti.getIndex(index.colNums);
		return new Btree(tran, ii);
	}

	private BtreeKey translate(BtreeKey key) {
		int intref = key.adr();
		assert IntRefs.isIntRef(intref);
		int adr = tran.getAdr(intref);
		assert adr != 0;
		return new BtreeKey(key.key, adr);
	}

	// -------------------------------------------------------------------------

	/** overridden by SchemaTransaction */
	protected void updateDbInfo() {
		dbinfo = db.state.dbinfo; // the latest
		updateDbInfo(updatedIndexes);
		assert schema == db.state.schema;
	}

	/**
	 * This is the final step that makes the commit permanent.
	 * An exception part way through this will be bad.
	 */
	private void finish() {
		try {
			Tran.StoreInfo info = tran.endStore();
			db.setState(db.state.dbinfoadr, dbinfo, schema, info.cksum, info.adr);
			commitTime = trans.clock();
			trans.commit(this);
			// db.persist(); // for testing - persist after every transaction
		} catch (Throwable e) {
			Errlog.fatal("ERROR in UpdateTransaction.finish", e);
		}
	}

	// end of commit =========================================================

	long asof() {
		return asof;
	}

	long commitTime() {
		return commitTime;
	}

	@Override
	public int readCount() {
		return reads.values().stream().mapToInt(tr -> tr.readCount()).sum();
	}

	@Override
	public int writeCount() {
		return writeCount;
	}

	// needed for PriorityQueue's in Transactions
	static final Comparator<UpdateTransaction> byCommit =
			(t1, t2) -> Long.compare(t1.commitTime, t2.commitTime);
	static final Comparator<UpdateTransaction> byAsof =
			(t1, t2) -> Long.compare(t1.asof, t2.asof);

	@Override
	public String toString() {
		return "ut" + num;
	}

}
