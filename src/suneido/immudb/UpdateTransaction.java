/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.hash.TIntHashSet;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;

import suneido.immudb.TranIndex.Iter;
import suneido.intfc.database.IndexIter;
import suneido.util.ThreadConfined;

import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.primitives.Shorts;

/**
 * Transactions must be thread confined.
 * They take a "snapshot" of the database state at the start
 * and then update the database state when they commit.
 * Storage is only written during commit.
 * Commit is single-threaded.
 */
@ThreadConfined
class UpdateTransaction extends ReadWriteTransaction {
	private final long asof;
	private volatile long commitTime = Long.MAX_VALUE;
	private final Map<Index,TransactionReads> reads = Maps.newHashMap();
	protected final TIntHashSet deletes = new TIntHashSet();
	/** used after commit by other transactions checking for conflicts */
	private final TIntHashSet inserts = new TIntHashSet();
	protected final Map<Index,TranIndex> updatedIndexes = Maps.newTreeMap(); //TODO why tree map?


	UpdateTransaction(int num, Database db) {
		super(num, db);
		asof = db.trans.clock();
	}

	@Override
	protected void lock(Database db) {
		assert ! locked;
		db.exclusiveLock.readLock().lock();
		locked = true;
	}

	@Override
	protected void unlock() {
		assert locked;
		db.exclusiveLock.readLock().unlock();
		locked = false;
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

	// used by foreign key cascade
	@Override
	public void updateAll(int tblnum, int[] colNums, Record oldkey, Record newkey) {
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
			updateRecord(tblnum, oldrec, rb.build());
		}
	}

	// used by foreign key cascade
	@Override
	public void removeAll(int tblnum, int[] colNums, Record key) {
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

	/** overrridden by SchemaTransaction */
	protected void trackReads(Index index, Iter iter) {
		((OverlayIndex.Iter) iter).trackRange(indexRange(index));
	}

	private IndexRange indexRange(Index index) {
		TransactionReads tr = reads.get(index);
		if (tr == null)
			reads.put(index, tr = new TransactionReads());
		IndexRange ir = new IndexRange();
		tr.add(ir);
		return ir;
	}

	// -------------------------------------------------------------------------

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
	public boolean isAborted() {
		return isEnded() && !isCommitted();
	}

	// commit ------------------------------------------------------------------

	private void buildReads() {
		for (TransactionReads tr : reads.values()) {
			tr.build();
		}
	}

	@Override
	protected void commit() {
		buildReads();
		synchronized(db.commitLock) {
			checkForConflicts();
			Tran.StoreInfo info = storeData();
			updateBtrees();
			updateDbInfo(info.cksum, info.adr);
			commitTime = trans.clock();
			trans.commit(this);
		}
	}

	protected void checkForConflicts() {
		Set<UpdateTransaction> overlapping = trans.getOverlapping(asof);
		for (UpdateTransaction t : overlapping) {
			assert t != this;
			TIntIterator iter = t.deletes.iterator();
			while (iter.hasNext()) {
				int del = iter.next();
				readValidation(del);
				checkForWriteConflict(del);
			}
			iter = t.inserts.iterator();
			while (iter.hasNext())
				readValidation(iter.next());
		}
	}

	// COULD make reads a Table<tblnum,index>
	// so you could easily get all the indexes for a tblnum
	private void readValidation(int adr) {
		Record r = input(adr);
		Table table = ck_getTable(r.tblnum);
		List<Index> indexes = table.indexesList();
		for (Index index : indexes) {
			TransactionReads tr = reads.get(index);
			if (tr == null)
				continue;
			Record key = key(r, index.colNums);
			if (tr.contains(key))
				throw new Conflict("read conflict in " + table.name);
		}
	}

	private static Record key(Record r, int[] colNums) {
		return new RecordBuilder().addFields(r, colNums).build();
	}

	private void checkForWriteConflict(int del) {
		if (deletes.contains(del))
			throw new Conflict("delete conflict");
	}

	// store data --------------------------------------------------------------

	protected Tran.StoreInfo storeData() {
		tran.startStore();
		Tran.StoreInfo info = null;
		try {
			// store removes first because
			// rebuild needs to process them first to avoid duplicate keys
			storeRemoves();
			storeAdds();
		} finally {
			info = tran.endStore();
		}
		return info;
	}

	private void storeRemoves() {
		int nr = deletes.size();
		int size = 2 * Shorts.BYTES + nr * Ints.BYTES;
		ByteBuffer buf = tran.dstor.buffer(tran.dstor.alloc(size));
		buf.putShort((short) tranType());
		assert nr < Short.MAX_VALUE;
		buf.putShort((short) nr);
		for (TIntIterator iter = deletes.iterator(); iter.hasNext(); ) {
			int adr = iter.next();
			buf.putInt(adr);
		}
	}

	/** overridden by SchemaTransaction */
	protected char tranType() {
		return 'u';
	}

	private void storeAdds() {
		int i = -1;
		for (Object x : tran.intrefs) {
			++i;
			if (x instanceof Record) { // add
				int intref = i | IntRefs.MASK;
				assert tran.intToRef(intref) == x;
				int adr = ((Record) x).store(tran.dstor);
				tran.setAdr(intref, adr);
				inserts.add(adr);
			}
		}
		markEndOfAdds(tran.dstor);
	}

	static void markEndOfAdds(Storage stor) {
		stor.buffer(stor.alloc(Shorts.BYTES)).putShort((short) -1);
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
			for (Record key : oti.removedKeys)
				global.remove(key);
			local = oti.local();
		}
		Btree.Iter iter = local.iterator();
		for (iter.next(); ! iter.eof(); iter.next()) {
			Record key = iter.curKey();
			if (false == global.add(translate(key), index.isKey, index.unique))
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

	private Record translate(Record key) {
		RecordBuilder rb = new RecordBuilder().addPrefix(key, key.size() - 1);
		int adr = BtreeNode.adr(key);
		assert IntRefs.isIntRef(adr);
		adr = tran.getAdr(adr);
		rb.adduint(adr);
		return rb.build();
	}

	// update dbinfo -----------------------------------------------------------

	/** overridden by SchemaTransaction */
	protected void updateDbInfo(int cksum, int adr) {
		dbinfo = db.state.dbinfo; // the latest
		updateDbInfo(updatedIndexes);
		assert dbstate.schema == db.state.schema;
		db.setState(dbinfo, db.state.schema, cksum, adr);
	}

	// end of commit =========================================================

	long asof() {
		return asof;
	}

	long commitTime() {
		return commitTime;
	}

	// needed for PriorityQueue's in Transactions
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
