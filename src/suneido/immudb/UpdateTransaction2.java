/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import gnu.trove.iterator.TIntIterator;

import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import suneido.SuException;
import suneido.immudb.Bootstrap.TN;
import suneido.immudb.IndexedData.Mode;
import suneido.intfc.database.IndexIter;
import suneido.util.ThreadConfined;

import com.google.common.collect.ImmutableList;
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
class UpdateTransaction2 extends ReadTransaction2 implements ImmuUpdateTran {
	protected final UpdateDbInfo udbinfo;
	protected boolean locked = false;
	private final long asof;
	private volatile long commitTime = Long.MAX_VALUE;
	private String conflict = null;
	private final boolean onlyReads = false;
	private final Map<Index,TransactionReads> reads = Maps.newHashMap();

	UpdateTransaction2(int num, Database2 db) {
		super(num, db);
		udbinfo = new UpdateDbInfo(stor, dbstate.dbinfo);
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
	public TableInfo getTableInfo(int tblnum) {
		return udbinfo.get(tblnum);
	}

	@Override
	protected TranIndex getIndex(IndexInfo info) {
		return new OverlayTranIndex(new Btree2(tran, info), new Btree2(tran));
	}

	/** for Bootstrap and TableBuilder */
	@Override
	public Btree2 addIndex(int tblnum, int... indexColumns) {
		assert locked;
		Btree2 btree = new Btree2(tran);
		indexes.put(index(tblnum, indexColumns), btree);
		return btree;
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
		udbinfo.updateRowInfo(tblnum, -1, -rec.bufSize());
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
System.out.println("COMMIT =====================");
		try {
			synchronized(db.commitLock) {
				//TODO read validation
				storeData();
				// use a read transaction to get access to global indexes
				ReadTransaction2 t = db.readonlyTran();
				try {
					updateBtrees(t);
					UpdateDbInfo udbi = new UpdateDbInfo(stor, t.rdbinfo.dbinfo);
					updateDbInfo(t.indexes, udbi);
					udbi.dbinfo().freeze();
					db.setState(new DatabaseState2(udbi.dbinfo(), db.state.schema));
				} finally {
					t.complete();
				}
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

	private void storeData() {
		tran.startStore();
		try {
			storeAdds();
			storeRemoves();
		} finally {
			tran.endStore();
		}
	}

	private void storeAdds() {
		int i = -1;
		for (Object x : tran.intrefs) {
			++i;
			if (x instanceof Record) { // add
System.out.print("store add " + x);
				int intref = i | IntRefs.MASK;
				assert (Record) tran.intToRef(intref) == x;
				tran.setAdr(intref, ((Record) x).store(tran.stor));
System.out.println(" @" + tran.getAdr(intref));
			}
		}
	}

	private void storeRemoves() {
		int nr = tran.removes.size();
		int size = Shorts.BYTES + (1 + nr) * Ints.BYTES;
		ByteBuffer buf = stor.buffer(stor.alloc(size));
		buf.putShort((short) 0xffff); // mark start of removes
		buf.putInt(nr);
		for (TIntIterator iter = tran.removes.iterator(); iter.hasNext(); ) {
			int adr = iter.next();
System.out.println("store remove " + adr);
			buf.putInt(adr);
		}
	}

	private void updateBtrees(ImmuReadTran t) {
		//TODO update in parallel
		for (Entry<Index, TranIndex> e : indexes.entrySet())
			updateBtree(t, e);
	}

	private void updateBtree(ImmuReadTran t, Entry<Index, TranIndex> e) {
		Index index = e.getKey();
		OverlayTranIndex oti = (OverlayTranIndex) e.getValue();
		TranIndex global = t.getIndex(index.tblnum, index.colNums);
		Btree2 local = oti.local();
		Btree2.Iter iter = local.iterator();
		for (iter.next(); ! iter.eof(); iter.next()) {
			Record key = iter.curKey();
			if (IntRefs.isIntRef(BtreeNode.adr(key)))
				global.add(translate(key), true); // TODO handle duplicate failure
			else
				global.remove(key);
		}
		((Btree2) global).freeze();
	}

	private Record translate(Record key) {
		RecordBuilder rb = new RecordBuilder().addPrefix(key, key.size() - 1);
		int adr = BtreeNode.adr(key);
		assert IntRefs.isIntRef(adr);
		adr = tran.getAdr(adr);
		rb.adduint(adr);
		return rb.build();
	}

	 static void updateDbInfo(Map<Index,TranIndex> indexes, UpdateDbInfo udbinfo) {
		if (indexes.isEmpty())
			return;
		Iterator<Entry<Index, TranIndex>> iter = indexes.entrySet().iterator();
		Entry<Index,TranIndex> e = iter.next();
		int tblnum = e.getKey().tblnum;
		do {
			// before table
			TableInfo ti = udbinfo.get(tblnum);
			// indexes
			ImmutableList.Builder<IndexInfo> b = ImmutableList.builder();
			do {
				Btree2 btree = (Btree2) e.getValue();
				b.add(new IndexInfo(e.getKey().colNums, btree.info()));
				if (! iter.hasNext())
					break;
				e = iter.next();
			} while (e.getKey().tblnum == tblnum);

			// after table
			ti = new TableInfo(
					tblnum, ti.nextfield, ti.nrows(), ti.totalsize(), b.build());
			udbinfo.add(ti);
		} while (iter.hasNext());
	}

	static final int INT_SIZE = 4;

	protected void store(int dbinfo, int redirs) {
		ByteBuffer buf = stor.buffer(stor.alloc(2 * INT_SIZE));
		buf.putInt(dbinfo);
		buf.putInt(redirs);
	}

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
		throw new SuException("transaction " + conflict);
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
