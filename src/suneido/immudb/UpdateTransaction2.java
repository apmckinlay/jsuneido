/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.hash.TObjectHashIterator;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TCustomHashSet;
import gnu.trove.strategy.HashingStrategy;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;

import suneido.SuException;
import suneido.immudb.Bootstrap.TN;
import suneido.immudb.IndexedData.Mode;
import suneido.intfc.database.IndexIter;
import suneido.util.ThreadConfined;

import com.google.common.collect.ImmutableList;
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
class UpdateTransaction2 extends ReadTransaction2 implements ImmuUpdateTran {
	protected boolean locked = false;
	private final long asof;
	private volatile long commitTime = Long.MAX_VALUE;
	private String conflict = null;
	private final boolean onlyReads = false;
//	private final Map<Index,TransactionReads> reads = Maps.newHashMap();
	private final TIntObjectHashMap<TableInfoDelta> tidelta =
			new TIntObjectHashMap<TableInfoDelta>();

	UpdateTransaction2(int num, Database2 db) {
		super(num, db);
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
	protected TranIndex getIndex(IndexInfo info) {
		return new OverlayTranIndex(new Btree2(tran, info), new Btree2(tran));
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
		updateRowInfo(tblnum, 1, rec.bufSize());
	}

	private void updateRowInfo(int tblnum, int nrows, int size) {
		tidelta(tblnum).update(nrows, size);
	}
	private TableInfoDelta tidelta(int tblnum) {
		TableInfoDelta d = tidelta.get(tblnum);
		if (d == null)
			tidelta.put(tblnum, d = new TableInfoDelta());
		return d;
	}

	@Override
	public int tableCount(int tblnum) {
		return getTableInfo(tblnum).nrows() + tidelta(tblnum).nrows;
	}

	@Override
	public long tableSize(int tblnum) {
		return getTableInfo(tblnum).totalsize() + tidelta(tblnum).size;
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
		updateRowInfo(tblnum, 0, to.bufSize() - from.bufSize());
	}

	// used by foreign key cascade
	@Override
	public void updateAll(int tblnum, int[] colNums, Record oldkey, Record newkey) {
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
		updateRowInfo(tblnum, -1, -rec.bufSize());
	}

	// used by foreign key cascade
	@Override
	public void removeAll(int tblnum, int[] colNums, Record key) {
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
				TranIndex btree = getIndex(index);
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

	// complete ================================================================

	// TODO if exception during commit, need to undo storage somehow
	// so crash recovery doesn't see it
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
				//TODO read validation
				int cksum = storeData();
				// use a read transaction to get access to global indexes
				ReadTransaction2 t = db.readonlyTran();
				try {
					updateBtrees(t);
					updateDbInfo(t, cksum);
				} finally {
					t.complete();
				}
				commitTime = trans.clock();
				trans.commit(this);
			}
		} catch(Conflict c) {
			conflict = c.toString();
		} finally {
			unlock();
		}
		return conflict;
	}

	// store data --------------------------------------------------------------

	protected int storeData() {
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
				tran.setAdr(intref, ((Record) x).store(tran.stor));
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

	protected void updateBtrees(ReadTransaction2 t) {
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

	protected void updateDbInfo(ReadTransaction2 t, int cksum) {
		UpdateDbInfo udbi = new UpdateDbInfo(db.state.dbinfo);
		updateDbInfo(t.indexes, udbi);
		udbi.dbinfo().freeze();
		db.setState(udbi.dbinfo(), db.state.schema, cksum);
	}

	protected void updateDbInfo(Map<Index,TranIndex> indexes, UpdateDbInfo udbi) {
		if (indexes.isEmpty())
			return;
		Iterator<Entry<Index, TranIndex>> iter = indexes.entrySet().iterator();
		Entry<Index,TranIndex> e = iter.next();
		do {
			// before table
			int tblnum = e.getKey().tblnum;
			TableInfo ti = udbi.get(tblnum);

			// indexes
			TCustomHashSet<IndexInfo> info = new TCustomHashSet<IndexInfo>(iihash);
			do {
				Btree2 btree = (Btree2) e.getValue();
				info.add(new IndexInfo(e.getKey().colNums, btree.info()));
				e = iter.hasNext() ? iter.next() : null;
			} while (e != null && e.getKey().tblnum == tblnum);
			for (IndexInfo ii : ti.indexInfo)
				info.add(ii); // no dups, so only adds ones not already there

			// after table
			assert ti.indexInfo.size() == info.size();
			TableInfoDelta d = tidelta(tblnum);
			ti = new TableInfo(tblnum, ti.nextfield,
					ti.nrows() + d.nrows, ti.totalsize() + d.size, toList(info));
			udbi.add(ti);
		} while (e != null);
	}

	private static ImmutableList<IndexInfo> toList(TCustomHashSet<IndexInfo> info) {
		ImmutableList.Builder<IndexInfo> b = ImmutableList.builder();
		TObjectHashIterator<IndexInfo> infoiter = info.iterator();
		while (infoiter.hasNext())
			b.add(infoiter.next());
		return b.build();
	}

	@SuppressWarnings("serial")
	private static HashingStrategy<IndexInfo> iihash = new HashingStrategy<IndexInfo>() {
			@Override
			public int computeHashCode(IndexInfo ii) {
				return Arrays.hashCode(ii.columns);
			}
			@Override
			public boolean equals(IndexInfo x, IndexInfo y) {
				return Arrays.equals(x.columns, y.columns);
			}
		};

	// end of complete =========================================================

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

	@Override
	public void abortThrow(String conflict) {
		this.conflict = conflict;
		abort();
		throw new Conflict(conflict);
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

	private static class TableInfoDelta {
		int nrows = 0;
		long size = 0;

		void update(int nrows, int size) {
			this.nrows += nrows;
			this.size += size;
		}
	}

	@Override
	public String toString() {
		return "ut" + num;
	}

}
