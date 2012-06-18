/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import gnu.trove.iterator.hash.TObjectHashIterator;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TCustomHashSet;
import gnu.trove.strategy.HashingStrategy;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import suneido.SuException;
import suneido.immudb.Bootstrap.TN;
import suneido.immudb.IndexedData.Mode;
import suneido.intfc.database.IndexIter;
import suneido.util.Print;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;

/**
 * Base class with common code for UpdateTransaction and BulkTransaction
 */
abstract class ReadWriteTransaction extends ReadTransaction {
	protected boolean locked = false;
	private String conflict = null;
	protected boolean onlyReads = true;
	protected final TIntObjectHashMap<TableInfoDelta> tidelta =
			new TIntObjectHashMap<TableInfoDelta>();
	protected final TIntObjectHashMap<IndexedData> indexedData =
			new TIntObjectHashMap<IndexedData>();

	ReadWriteTransaction(int num, Database db) {
		super(num, db);
		lock(db);
	}

	abstract void lock(Database db);
	abstract void unlock();

	// add ---------------------------------------------------------------------

	@Override
	public void addRecord(String table, suneido.intfc.database.Record r) {
		Table tbl = getTable(table);
		DataRecord rec = truncateRecord(tbl.num, r);
		addRecord(tbl.num, rec);
		callTrigger(tbl, null, rec); // must be final step - may throw
	}

	int addRecord(int tblnum, DataRecord rec) {
		check(tblnum, "output");
		onlyReads = false;
		rec.tblnum(tblnum);
		int adr = indexedData(tblnum).add(rec);
		updateRowInfo(tblnum, 1, rec.bufSize());
		return adr;
	}

	/** for client-server extend bug */
	private DataRecord truncateRecord(int tblnum, suneido.intfc.database.Record r) {
		if (tblnum > 3) {
			TableInfo ti = getTableInfo(tblnum);
			if (r.size() > ti.nextfield)
				return new RecordBuilder().addAll(r).truncate(ti.nextfield).build();
		}
		return (DataRecord) r;
	}

	// update ------------------------------------------------------------------

	@Override
	public int updateRecord(int fromadr, suneido.intfc.database.Record to) {
		if (fromadr == 1)
			throw new SuException("can't update the same record multiple times");
		DataRecord from = tran.getrec(fromadr);
		updateRecord(from.tblnum(), from, to);
		return 1; // don't know record address till commit
	}

	@Override
	public int updateRecord(int tblnum,
			suneido.intfc.database.Record from,
			suneido.intfc.database.Record r) {
		DataRecord to = truncateRecord(tblnum, r);
		updateRecord2(tblnum, (DataRecord) from, to);
		// must be final step - may throw
		callTrigger(ck_getTable(tblnum), from, to);
		return 1; // don't know record address till commit
	}

	int updateRecord2(int tblnum, DataRecord from, DataRecord to) {
		check(tblnum, "update");
		onlyReads = false;
		to.tblnum(tblnum);
		int adr = indexedData(tblnum).update(from, to);
		updateRowInfo(tblnum, 0, to.bufSize() - from.bufSize());
		return adr;
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

	// remove ------------------------------------------------------------------

	@Override
	public void removeRecord(int adr) {
		DataRecord rec = tran.getrec(adr);
		removeRecord(rec.tblnum(), rec);
	}

	@Override
	public void removeRecord(int tblnum, suneido.intfc.database.Record rec) {
		removeRecord(tblnum, (Record) rec);
	}

	protected int removeRecord(int tblnum, Record rec) {
		check(tblnum, "delete");
		onlyReads = false;
		int adr = indexedData(tblnum).remove(rec);
		callTrigger(ck_getTable(tblnum), rec, null);
		updateRowInfo(tblnum, -1, -rec.bufSize());
		return adr;
	}

	// used by foreign key cascade
	void removeAll(int tblnum, int[] colNums, Record key) {
		IndexIter iter = getIndex(tblnum, colNums).iterator(key);
		for (iter.next(); ! iter.eof(); iter.next())
			removeRecord(iter.keyadr());
	}

	private void check(int tblnum, String op) {
		checkNotEnded(op);
		checkNotSystemTable(tblnum, op);
	}
	private void checkNotEnded(String op) {
		if (isEnded())
			throw new SuException(this + " " + op + " on ended transaction");
	}
	protected void checkNotSystemTable(int tblnum, String op) {
		if (tblnum <= TN.VIEWS)
			throw new SuException("can't " + op + " system table ");
	}

	// -------------------------------------------------------------------------

	protected IndexedData indexedData(int tblnum) {
		IndexedData id = indexedData.get(tblnum);
		if (id == null)
			indexedData.put(tblnum, id = indexedData2(tblnum));
		return id;
	}

	/** overridden by UpdateTransaction */
	protected IndexedData indexedData2(int tblnum) {
		IndexedData id = new IndexedData(this);
		Table table = getTable(tblnum);
		if (table == null) {
			int[] indexColumns = Bootstrap.indexColumns[tblnum];
			TranIndex btree = getIndex(tblnum, indexColumns);
			id.index(btree, Mode.KEY, indexColumns);
		} else {
			for (Index index : getTable(tblnum).indexes) {
				TranIndex btree = getIndex(index);
				String colNames = table.numsToNames(index.colNums);
				indexedDataIndex(id, table, index, btree, colNames);
			}
		}
		return id;
	}

	/** overridden by DbRebuild.RebuildTransaction */
	protected void indexedDataIndex(IndexedData id, Table table, Index index,
			TranIndex btree, String colNames) {
		id.index(btree, index.mode(), index.colNums, colNames,
				index.fksrc, schema.getFkdsts(table.name, colNames));
	}

	protected void updateRowInfo(int tblnum, int nrows, int size) {
		tidelta(tblnum).update(nrows, size);
	}
	private TableInfoDelta tidelta(int tblnum) {
		TableInfoDelta d = tidelta.get(tblnum);
		if (d == null)
			tidelta.put(tblnum, d = new TableInfoDelta());
		return d;
	}
	private static class TableInfoDelta {
		int nrows = 0;
		long size = 0;

		void update(int nrows, int size) {
			this.nrows += nrows;
			this.size += size;
		}
	}

	protected static class Conflict extends SuException {
			private static final long serialVersionUID = 1L;

			Conflict(String explanation) {
				super("transaction conflict: " + explanation);
			}
		}

	@Override
	public int tableCount(int tblnum) {
		// PERF don't create tidelta
		return getTableInfo(tblnum).nrows() + tidelta(tblnum).nrows;
	}

	@Override
	public long tableSize(int tblnum) {
		// PERF don't create tidelta
		return getTableInfo(tblnum).totalsize() + tidelta(tblnum).size;
	}

	void abortThrow(String conflict) {
		conflict = "aborted: " + this + " - " + conflict;
		Print.timestamped(conflict);
		abort(conflict);
		throw new SuException(conflict);
	}

	@Override
	public String conflict() {
		return conflict ;
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

	void abort(String message) {
		conflict = message;
		abort();
	}

	@Override
	public void abort() {
		if (isEnded())
			return;
		super.abort();
		unlock();
	}

	// complete ----------------------------------------------------------------

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
			commit();
		} catch(Conflict c) {
			conflict = c.toString();
			abort();
		} finally {
			if (locked)
				unlock();
		}
		return conflict;
	}

	protected abstract void commit();

	/** Update dbinfo with tidelta and index information and then freezes it */
	protected void updateDbInfo(TreeMap<Index,TranIndex> indexes) {
		if (indexes.isEmpty())
			return;
		Iterator<Entry<Index, TranIndex>> iter = indexes.entrySet().iterator();
		Entry<Index,TranIndex> e = iter.next();
		do {
			// before table
			int tblnum = e.getKey().tblnum;
			TableInfo ti = (TableInfo) dbinfo.get(tblnum);

			// indexes
			TCustomHashSet<IndexInfo> info = new TCustomHashSet<IndexInfo>(iihash);
			do {
				Btree btree = (Btree) e.getValue();
				info.add(new IndexInfo(e.getKey().colNums, btree.info()));
				e = iter.hasNext() ? iter.next() : null;
			} while (e != null && e.getKey().tblnum == tblnum);
			for (IndexInfo ii : ti.indexInfo)
				info.add(ii); // no dups, so only adds ones not already there

			// after table
			TableInfoDelta d = tidelta(tblnum);
			assert ! info.isEmpty();
			ti = new TableInfo(tblnum, ti.nextfield,
					ti.nrows() + d.nrows, ti.totalsize() + d.size, toList(info));
			dbinfo = dbinfo.with(ti);
		} while (e != null);
		dbinfo.freeze();
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

	private static ImmutableList<IndexInfo> toList(TCustomHashSet<IndexInfo> info) {
		ImmutableList.Builder<IndexInfo> b = ImmutableList.builder();
		TObjectHashIterator<IndexInfo> infoiter = info.iterator();
		while (infoiter.hasNext())
			b.add(infoiter.next());
		return b.build();
	}

}
