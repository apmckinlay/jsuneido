/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.TreeMap;

import suneido.SuException;
import suneido.immudb.Bootstrap.TN;
import suneido.intfc.database.HistoryIterator;
import suneido.intfc.database.IndexIter;
import suneido.util.ThreadConfined;

import com.google.common.collect.Maps;

/**
 * Effectively immutable, but indexes are cached.
 * Transactions must be thread confined.
 * They take a "snapshot" of the database state at the start.
 * ReadTransactions require no locking
 * since they only operate on immutable data.
 */
@ThreadConfined
class ReadTransaction implements suneido.intfc.database.Transaction {
	protected final int num;
	protected final Database db;
	protected final Tran tran;
	protected final Database.State dbstate;
	protected DbHashTrie dbinfo;
	protected Tables schema; // not final - modified by SchemaTran
	/** needs to be ordered for ReadWriteTransaction updateDbInfo */
	protected final TreeMap<Index,TranIndex> indexes = Maps.newTreeMap();
	protected final Transactions trans;
	private boolean ended = false;

	ReadTransaction(int num, Database db) {
		this.num = num;
		this.db = db;
		dbstate = db.state; // don't inline, read only once
		schema = dbstate.schema;
		dbinfo = dbstate.dbinfo;
		tran = new Tran(db.dstor, db.istor);
		trans = db.trans;
		trans.add(this);
	}

	Set<ForeignKeyTarget> getForeignKeys(String tableName, String colNames) {
		return schema.getFkdsts(tableName, colNames);
	}

	/** if colNames is null returns firstIndex */
	TranIndex getIndex(int tblnum, String colNames) {
		return getIndex(index(tblnum, colNames));
	}

	TranIndex getIndex(int tblnum, int... colNums) {
		return getIndex(index(tblnum, colNums));
	}

	protected TranIndex getIndex(Index index) {
		TranIndex btree = indexes.get(index);
		if (btree != null)
			return btree;
		TableInfo ti = getTableInfo(index.tblnum);
		btree = getIndex(ti.getIndex(index.colNums));
		indexes.put(index, btree);
		return btree;
	}

	/** Overridden in UpdateTransaction */
	protected TranIndex getIndex(IndexInfo info) {
		return new Btree(tran, info);
	}

	boolean hasIndex(int tblnum, int[] colNums) {
		Index index = index(tblnum, colNums);
		return index != null && indexes.containsKey(index);
	}

	protected Index index(int tblnum, int[] colNums) {
		return index(getTable(tblnum), tblnum, colNums);
	}

	protected Index index(int tblnum, String colNames) {
		Table table = ck_getTable(tblnum);
		int[] colNums = (colNames == null)
			? table.firstIndex().colNums
			: table.namesToNums(colNames);
		return index(table, tblnum, colNums);
	}

	static final Index tables_index =
			new Index(TN.TABLES, Bootstrap.indexColumns[TN.TABLES]);
	static final Index columns_index =
			new Index(TN.COLUMNS, Bootstrap.indexColumns[TN.COLUMNS]);
	static final Index indexes_index =
			new Index(TN.INDEXES, Bootstrap.indexColumns[TN.INDEXES]);

	/**
	 * Complicated by bootstrapping because schema tables aren't in schema.
	 * @return A map key for an index.
	 */
	private static Index index(Table table, int tblnum, int[] colNums) {
		if (table == null)
			switch (tblnum) {
			case TN.TABLES: return tables_index;
			case TN.COLUMNS: return columns_index;
			case TN.INDEXES: return indexes_index;
		}
		return table == null ? null : table.getIndex(colNums);
	}

	// used for fetching view definitions
	DataRecord lookup(int tblnum, int[] colNums, Record key) {
		TranIndex btree = getIndex(tblnum, colNums);
		int adr = btree.get(key);
		if (adr == 0)
			return null; // not found
		return input(adr);
	}

	@Override
	public DataRecord lookup(
			int tblnum, String index, suneido.intfc.database.Record key) {
		TranIndex bti = getIndex(tblnum, index);
		if (bti == null)
			return null;
		IndexIter iter = iter(tblnum, index, key, key);
		iter.next();
		return iter.eof() ? null : input(iter.keyadr());
	}

	boolean exists(int tblnum, int[] colNums, Record key) {
		return 0 != getIndex(tblnum, colNums).get(key);
	}

	@Override
	public Table getTable(String tableName) {
		return schema.get(tableName);
	}

	@Override
	public Table getTable(int tblnum) {
		// schema is null while loading the schema
		return schema == null ? null : schema.get(tblnum);
	}

	/** @return The table info as of the start of the transaction */
	TableInfo getTableInfo(int tblnum) {
		return (TableInfo) dbinfo.get(tblnum);
	}

	/** @return view definition, else null if view not found */
	@Override
	public String getView(String name) {
		return Views.getView(this, name);
	}

	@Override
	public boolean isReadonly() {
		return true;
	}

	@Override
	public boolean isReadWrite() {
		return false;
	}

	@Override
	public boolean isEnded() {
		return ended;
	}

	@Override
	public String conflict() {
		return "";
	}

	@Override
	public boolean tableExists(String table) {
		return getTable(table) != null;
	}

	@Override
	public Table ck_getTable(String tablename) {
		Table tbl = getTable(tablename);
		if (tbl == null)
			throw new SuException("nonexistent table: " + tablename);
		return tbl;
	}

	@Override
	public Table ck_getTable(int tblnum) {
		Table tbl = getTable(tblnum);
		if (tbl == null)
			throw new SuException("nonexistent table: " + tblnum);
		return tbl;
	}

	@Override
	public int tableCount(int tblnum) {
		return getTableInfo(tblnum).nrows();
	}

	@Override
	public long tableSize(int tblnum) {
		return getTableInfo(tblnum).totalsize();
	}

	@Override
	public int indexSize(int tblnum, String columns) {
		return getIndex(tblnum, columns).totalSize();
	}

	@Override
	public int keySize(int tblnum, String columns) {
		int nrecs = tableCount(tblnum);
		if (nrecs == 0)
			return 0;
		TranIndex idx = getIndex(tblnum, columns);
		return idx.totalSize() / nrecs;
	}

	@Override
	public float rangefrac(int tblnum, String columns,
			suneido.intfc.database.Record from, suneido.intfc.database.Record to) {
		float frac = getIndex(tblnum, columns).rangefrac((Record) from, (Record) to);
		return frac < .001f ? .001f : frac;
	}

	@Override
	public void abortIfNotComplete() {
		if (! isEnded())
			abort();
	}

	@Override
	public void abort() {
		end();
	}

	@Override
	public synchronized void ck_complete() {
		String s = complete();
		if (s != null)
			throw new SuException("transaction commit failed: " + s);
	}

	@Override
	public String complete() {
		end();
		return null;
	}

	private void end() {
		if (isEnded())
			return;
		trans.abort(this);
		ended = true;
	}

	@Override
	public void addRecord(String table, suneido.intfc.database.Record r) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int updateRecord(int recadr, suneido.intfc.database.Record rec) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int updateRecord(int tblnum, suneido.intfc.database.Record oldrec,
			suneido.intfc.database.Record newrec) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeRecord(int off) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeRecord(int tblnum, suneido.intfc.database.Record rec) {
		throw new UnsupportedOperationException();
	}

	@Override
	public DataRecord input(int adr) {
		return tran.getrec(adr);
	}

	@Override
	public void callTrigger(suneido.intfc.database.Table table,
			suneido.intfc.database.Record oldrec,
			suneido.intfc.database.Record newrec) {
		if (table != null)
			db.callTrigger(this, (Table) table, (Record) oldrec, (Record) newrec);
	}

	@Override
	public int num() {
		return num;
	}

	@Override
	public Record fromRef(Object ref) {
		return ref instanceof Integer
				? tran.getrec((Integer) ref)
				: Record.from((ByteBuffer) ref);
	}

	@Override
	public HistoryIterator historyIterator(int tblnum) {
		//TODO history
		throw new UnsupportedOperationException();
	}

	@Override
	public IndexIter iter(int tblnum, String columns) {
		return getIndex(tblnum, columns).iterator();
	}

	//PERF if key and org == end, could use get instead of iterator
	@Override
	public IndexIter iter(int tblnum, String columns,
			suneido.intfc.database.Record org, suneido.intfc.database.Record end) {
		return getIndex(tblnum, columns).iterator((Record) org, (Record) end);
	}

	@Override
	public IndexIter iter(int tblnum, String columns, IndexIter iter) {
		return getIndex(tblnum, columns).iterator(iter);
	}

	@Override
	public String toString() {
		return "rt" + num;
	}

	@Override
	public boolean isAborted() {
		return false;
	}

	Tran tran() {
		return tran;
	}

	/** used by TableBuilder */
	SchemaTransaction schemaTran() {
		return db.schemaTransaction();
	}

}
