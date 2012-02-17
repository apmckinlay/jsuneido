/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;

import suneido.SuException;
import suneido.immudb.Bootstrap.TN;
import suneido.intfc.database.HistoryIterator;
import suneido.intfc.database.IndexIter;
import suneido.util.ThreadConfined;

import com.google.common.collect.Maps;

/**
 * Effectively immutable, but indexes are cached
 * Transactions must be thread confined.
 * They take a "snapshot" of the database state at the start.
 * ReadTransactions require no locking
 * since they only operate on immutable data.
 */
@ThreadConfined
class ReadTransaction2 implements ImmuReadTran, Locking {
	protected final int num;
	protected final Database2 db;
	protected final Storage stor;
	protected final Tran tran;
	protected final DatabaseState2 dbstate;
	protected final ReadDbInfo rdbinfo;
	protected final Tables schema;
	protected final Map<Index,TranIndex> indexes = Maps.newTreeMap();
	protected final Transactions2 trans;
	private boolean ended = false;

	ReadTransaction2(int num, Database2 db) {
		this.num = num;
		this.db = db;
		stor = db.stor;
		dbstate = db.state;
		schema = dbstate.schema;
		rdbinfo = new ReadDbInfo(dbstate.dbinfo);
		tran = new Tran(stor, new Redirects(DbHashTrie.empty(stor)));
		trans = db.trans;
		trans.add(this);
	}

	protected ReadDbInfo dbinfo() {
		return rdbinfo;
	}

	@Override
	public Set<ForeignKeyTarget> getForeignKeys(String tableName, String colNames) {
		return dbstate.schema.getFkdsts(tableName, colNames);
	}

	/** if colNames is null returns firstIndex */
	TranIndex getIndex(int tblnum, String colNames) {
		Table tbl = ck_getTable(tblnum);
		int[] colNums = (colNames == null)
			? tbl.firstIndex().colNums
			: tbl.namesToNums(colNames);
		return getIndex(tblnum, colNums);
	}

	@Override
	public TranIndex getIndex(int tblnum, int... colNums) {
		Index index = index(tblnum, colNums);
		TranIndex btree = indexes.get(index);
		if (btree != null)
			return btree;
		TableInfo ti = getTableInfo(tblnum);
		btree = getIndex(ti.getIndex(colNums));
		indexes.put(index, btree);
		return btree;
	}

	/** Overridden in UpdateTransaction */
	protected TranIndex getIndex(IndexInfo info) {
		return new Btree2(tran, info);
	}

	@Override
	public boolean hasIndex(int tblnum, int[] colNums) {
		return indexes.containsKey(index(tblnum, colNums));
	}

	private static final Index tables_index =
			new Index(TN.TABLES, Bootstrap.indexColumns[TN.TABLES]);
	private static final Index columns_index =
			new Index(TN.COLUMNS, Bootstrap.indexColumns[TN.COLUMNS]);
	private static final Index indexes_index =
			new Index(TN.INDEXES, Bootstrap.indexColumns[TN.INDEXES]);

	/**
	 * Complicated by bootstrapping because schema tables aren't in schema.
	 * @return A map key for an index.
	 */
	protected Index index(int tblnum, int[] colNums) {
		switch (tblnum) {
		case TN.TABLES: return tables_index;
		case TN.COLUMNS: return columns_index;
		case TN.INDEXES: return indexes_index;
		default:
			Table table = schema.get(tblnum);
			return table == null ? null : table.getIndex(colNums);
		}

	}

	// used for fetching view definitions
	@Override
	public Record lookup(int tblnum, int[] colNums, Record key) {
		TranIndex btree = getIndex(tblnum, colNums);
		int adr = btree.get(key);
		if (adr == 0)
			return null; // not found
		return input(adr);
	}

	@Override
	public Record lookup(
			int tblnum, String index, suneido.intfc.database.Record key) {
		TranIndex bti = getIndex(tblnum, index);
		if (bti == null)
			return null;
		IndexIter iter = bti.iterator((Record) key);
		iter.next();
		return iter.eof() ? null : input(iter.keyadr());
	}

	@Override
	public boolean exists(int tblnum, int[] colNums, Record key) {
		return 0 != getIndex(tblnum, colNums).get(key);
	}

	@Override
	public Table getTable(String tableName) {
		return schema.get(tableName);
	}

	@Override
	public Table getTable(int tblnum) {
		return schema.get(tblnum);
	}

	@Override
	public TableInfo getTableInfo(int tblnum) {
		return rdbinfo.get(tblnum);
	}

	/** @return view definition, else null if view not found */
	@Override
	public String getView(String name) {
		return null; //Views.getView(this, name);
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
	public Record input(int adr) {
		return tran.getrec(adr);
	}

	@Override
	public void callTrigger(suneido.intfc.database.Table table,
			suneido.intfc.database.Record oldrec,
			suneido.intfc.database.Record newrec) {
//		if (table != null)
//			db.callTrigger(this, (Table) table, (Record) oldrec, (Record) newrec);
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
		throw new UnsupportedOperationException();
	}

	/** if columns is null returns firstIndex */
	@Override
	public IndexIter iter(int tblnum, String columns) {
		return getIndex(tblnum, columns).iterator();
	}

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
	public void readLock(int adr) {
	}

	@Override
	public void writeLock(int adr) {
	}

	@Override
	public String toString() {
		return "rt" + num;
	}

	@Override
	public boolean isAborted() {
		return false;
	}

	@Override
	public Tran tran() {
		return tran;
	}

	@Override
	public ExclusiveTransaction2 exclusiveTran() {
		return db.exclusiveTran();
	}

}
