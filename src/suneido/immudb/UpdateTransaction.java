/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
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
	/** updated by IndexedData, used by OverlayIndex */
	protected final TIntHashSet deletes = new TIntHashSet();
	/** used after commit by other transactions checking for conflicts */
	private final TIntHashSet inserts = new TIntHashSet();
	/** needs to be ordered tree for ReadWriteTransaction updateDbInfo */
	protected final TreeMap<Index,TranIndex> updatedIndexes = Maps.newTreeMap();
	private final TIntArrayList actions = new TIntArrayList();


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

	@Override
	public int addRecord(int tblnum, Record rec) {
		int adr = super.addRecord(tblnum, rec);
		actions.add(adr);
//System.out.println("addRecord " + adr + " = " + rec);
		return adr;
	}

	@Override
	public int updateRecord(int tblnum, Record from, Record to) {
		to.address = tran.refToInt(to);
		int fromadr = super.updateRecord(tblnum, from, to);
		actions.add(UPDATE);
		actions.add(fromadr);
		actions.add(to.address);
		return fromadr;
	}

	@Override
	public int removeRecord(int tblnum, Record rec) {
		int adr = super.removeRecord(tblnum, rec);
		if (IntRefs.isIntRef(adr))
			tran.update(adr, null); // don't record delete of new record
		else
			actions.add(adr);
		return adr;
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
//System.out.println("UpdateTran commit");
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
			startOfCommit();
			storeActions();
//			storeRemoves();
//			storeAdds();
			endOfCommit(tran.dstor);
		} finally {
			info = tran.endStore();
		}
		return info;
	}

	protected void startOfCommit() {
		ByteBuffer buf = tran.dstor.buffer(tran.dstor.alloc(1));
		buf.put((byte) tranType());
	}

	/** overridden by SchemaTransaction */
	protected char tranType() {
		return 'u';
	}

	private void storeActions() {
		short type = REMOVE;
		for (TIntIterator iter = actions.iterator(); iter.hasNext(); ) {
			int act = iter.next();
			if (act == UPDATE)
				type = UPDATE;
			else if (IntRefs.isIntRef(act)) { //  add
				if (tran.intToRef(act) != null) { // don't store if deleted
					int adr = ((Record) tran.intToRef(act)).store(tran.dstor);
					tran.setAdr(act, adr);
					inserts.add(adr);
//System.out.println("add " + act + " = " + adr + " = " + tran.intToRef(act));
				}
			} else { // remove
				ByteBuffer buf = tran.dstor.buffer(
						tran.dstor.alloc(Shorts.BYTES + Ints.BYTES));
				buf.putShort(type);
				buf.putInt(act);
				type = REMOVE;
			}
		}
	}

//	private void storeRemoves() {
//		for (TIntIterator iter = deletes.iterator(); iter.hasNext(); ) {
//			ByteBuffer buf = tran.dstor.buffer(tran.dstor.alloc(Shorts.BYTES + Ints.BYTES));
//			int adr = iter.next();
//			buf.putShort(REMOVE);
//			buf.putInt(adr);
//		}
//	}
//
//	private void storeAdds() {
//		int i = -1;
//		for (Object x : tran.intrefs) {
//			++i;
//			if (x instanceof Record) { // add
//				int intref = i | IntRefs.MASK;
//				assert tran.intToRef(intref) == x;
//				int adr = ((Record) x).store(tran.dstor);
//				tran.setAdr(intref, adr);
//				inserts.add(adr);
//			}
//		}
//	}

	static void endOfCommit(Storage stor) {
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
		int intref = BtreeNode.adr(key);
		assert IntRefs.isIntRef(intref);
		int adr = tran.getAdr(intref);
//System.out.println("translate " + intref + " = " + adr + " in " + key);
		assert adr != 0;
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
