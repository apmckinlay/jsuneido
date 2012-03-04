/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.hash.TIntHashSet;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;

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
class UpdateTransaction2 extends ReadWriteTransaction implements ImmuUpdateTran {
	private final long asof;
	private volatile long commitTime = Long.MAX_VALUE;
	private final Map<Index,TransactionReads> reads = Maps.newHashMap();
	private final TIntHashSet inserts = new TIntHashSet();
	private final TIntHashSet deletes = new TIntHashSet();

	UpdateTransaction2(int num, Database2 db) {
		super(num, db);
		asof = db.trans.clock();
	}

	@Override
	protected void lock(Database2 db) {
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
	protected TranIndex getIndex(IndexInfo info) {
		return new OverlayTranIndex(new Btree2(tran, info), new Btree2(tran));
	}

	@Override
	public int updateRecord(int tblnum, Record from, Record to) {
		int adr = super.updateRecord(tblnum, from, to);
		trackDelete(adr);
		return adr;
	}

	@Override
	public int removeRecord(int tblnum, Record rec) {
		int adr = super.removeRecord(tblnum, rec);
		trackDelete(adr);
		return adr;
	}

	private void trackDelete(int adr) {
		assert adr != 0;
		if (! IntRefs.isIntRef(adr))
			deletes.add(adr);
	}

	@Override
	public IndexIter iter(int tblnum, String columns) {
		Index index = index(tblnum, columns);
		trackReads(index, DatabasePackage2.MIN_RECORD, DatabasePackage2.MAX_RECORD);
		return getIndex(index).iterator();
	}

	//TODO track actual range read
	private void trackReads(Index index, Record from, Record to) {
		TransactionReads tr = reads.get(index);
		if (tr == null)
			reads.put(index, tr = new TransactionReads());
		tr.add(new IndexRange(from, to));
	}

	@Override
	public IndexIter iter(int tblnum, String columns,
			suneido.intfc.database.Record org, suneido.intfc.database.Record end) {
		Index index = index(tblnum, columns);
		trackReads(index, (Record) org, (Record) end);
		return getIndex(index).iterator((Record) org, (Record) end);
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
			int cksum = storeData();
			// use a read transaction to get access to global indexes
			ReadTransaction2 t = db.readTransaction();
			try {
				updateBtrees(t);
				updateDbInfo(t, cksum);
			} finally {
				t.complete();
			}
			commitTime = trans.clock();
			trans.commit(this);
		}
	}

	private void checkForConflicts() {
		Set<UpdateTransaction2> overlapping = trans.getOverlapping(asof);
		for (UpdateTransaction2 t : overlapping) {
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
				throw new Conflict("read conflict");
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

	private int storeData() {
		tran.startStore();
		int cksum = 0;
		try {
			storeAdds();
			storeRemoves();
		} finally {
			cksum = tran.endStore();
		}
		return cksum;
	}

	private void storeAdds() {
		int i = -1;
		for (Object x : tran.intrefs) {
			++i;
			if (x instanceof Record) { // add
				int intref = i | IntRefs.MASK;
				assert tran.intToRef(intref) == x;
				int adr = ((Record) x).store(tran.stor);
				tran.setAdr(intref, adr);
				inserts.add(adr);
			}
		}
	}

	private void storeRemoves() {
		int nr = tran.removes.size();
		int size = Shorts.BYTES + (1 + nr) * Ints.BYTES;
		ByteBuffer buf = tran.stor.buffer(tran.stor.alloc(size));
		buf.putShort((short) 0xffff); // mark start of removes
		buf.putInt(nr);
		for (TIntIterator iter = tran.removes.iterator(); iter.hasNext(); ) {
			int adr = iter.next();
			buf.putInt(adr);
		}
	}

	// update btrees -----------------------------------------------------------

	private void updateBtrees(ReadTransaction2 t) {
		//PERF update in parallel
		for (Entry<Index, TranIndex> e : indexes.entrySet())
			updateBtree(t, e);
	}

	private void updateBtree(ReadTransaction2 t, Entry<Index, TranIndex> e) {
		Index index = e.getKey();
		OverlayTranIndex oti = (OverlayTranIndex) e.getValue();
		Btree2 global = (Btree2) t.getIndex(index);
		Btree2 local = oti.local();
		Btree2.Iter iter = local.iterator();
		boolean updated = false;
		for (iter.next(); ! iter.eof(); iter.next()) {
			Record key = iter.curKey();
			if (IntRefs.isIntRef(BtreeNode.adr(key))) {
				if (false == global.add(translate(key), index.isKey, index.unique))
					throw new Conflict("duplicate key");
			} else
				global.remove(key);
			updated = true;
		}
		if (updated)
			global.freeze();
		else
			t.indexes.remove(index); // don't need to update dbinfo
		assert global.frozen();
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

	private void updateDbInfo(ReadTransaction2 t, int cksum) {
		UpdateDbInfo udbi = new UpdateDbInfo(db.state.dbinfo);
		updateDbInfo(t.indexes, udbi);
		udbi.dbinfo().freeze();
		db.setState(udbi.dbinfo(), db.state.schema, cksum);
	}

	// end of commit =========================================================

	long asof() {
		return asof;
	}

	long commitTime() {
		return commitTime;
	}

	// needed for PriorityQueue's in Transactions
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
