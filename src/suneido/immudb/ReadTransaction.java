/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;
import java.util.Set;

import suneido.intfc.database.HistoryIterator;
import suneido.intfc.database.IndexIter;
import suneido.util.ThreadConfined;

import com.google.common.collect.HashBasedTable;

/**
 * Effectively immutable, but indexes are cached
 * Transactions must be thread confined.
 * They take a "snapshot" of the database state at the start.
 * ReadTransactions require no locking
 * since they only operate on immutable data.
 */
@ThreadConfined
class ReadTransaction implements suneido.intfc.database.Transaction, Locking {
	protected final int num;
	protected final Database db;
	protected final Storage stor;
	protected final Tran tran;
	protected final ReadDbInfo rdbinfo;
	protected final Tables schema;
	protected final com.google.common.collect.Table<Integer,ColNums,Btree> indexes;

	ReadTransaction(int num, Database db) {
		this.num = num;
		this.db = db;
		stor = db.stor;
		rdbinfo = new ReadDbInfo(db.getDbinfo());
		schema = db.schema;
		tran = new Tran(stor, new Redirects(db.getRedirs()));
		indexes = HashBasedTable.create();
	}

	protected ReadDbInfo dbinfo() {
		return rdbinfo;
	}

	Set<ForeignKeyTarget> getForeignKeys(String tableName, String colNames) {
		return schema.getFkdsts(tableName, colNames);
	}

	/** if colNames is null returns firstIndex */
	Btree getIndex(int tblnum, String colNames) {
		Table tbl = ck_getTable(tblnum);
		int[] fields = (colNames == null)
			? tbl.firstIndex().colNums
			: tbl.namesToNums(colNames);
		return getIndex(tblnum, fields);
	}

	Btree getIndex(int tblnum, int... indexColumns) {
		ColNums colNums = new ColNums(indexColumns);
		Btree btree = indexes.get(tblnum, colNums);
		if (btree != null)
			return btree;
		TableInfo ti = getTableInfo(tblnum);
		btree = new Btree(tran, this, ti.getIndex(indexColumns));
		indexes.put(tblnum, colNums, btree);
		return btree;
	}

	boolean hasIndex(int tblnum, int[] indexColumns) {
		return indexes.contains(tblnum, new ColNums(indexColumns));
	}

	Record getrec(int adr) {
		return tran.getrec(adr);
	}

	Record lookup(int tblnum, int[] indexColumns, Record key) {
		Btree btree = getIndex(tblnum, indexColumns);
		int adr = btree.get(key);
		if (adr == 0)
			return null; // not found
		return getrec(adr);
	}

	@Override
	public Record lookup(
			int tblnum, String index, suneido.intfc.database.Record key) {
		Btree bti = getIndex(tblnum, index);
		if (bti == null)
			return null;
		Btree.Iter iter = bti.iterator((Record) key);
		iter.next();
		return iter.eof() ? null : input(iter.keyadr());
	}

	boolean exists(int tblnum, int[] indexColumns, Record key) {
		return 0 != getIndex(tblnum, indexColumns).get(key);
	}

	@Override
	public Table getTable(String tableName) {
		return schema.get(tableName);
	}

	@Override
	public Table getTable(int tblnum) {
		return schema.get(tblnum);
	}

	TableInfo getTableInfo(int tblnum) {
		return rdbinfo.get(tblnum);
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
		return false;
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
			throw new RuntimeException("nonexistent table: " + tablename);
		return tbl;
	}

	@Override
	public Table ck_getTable(int tblnum) {
		Table tbl = getTable(tblnum);
		if (tbl == null)
			throw new RuntimeException("nonexistent table: " + tblnum);
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
		Btree idx = getIndex(tblnum, columns);
		return idx.totalSize() / nrecs;
	}

	@Override
	public float rangefrac(int tblnum, String columns,
			suneido.intfc.database.Record from, suneido.intfc.database.Record to) {
		return getIndex(tblnum, columns).rangefrac((Record) from, (Record) to);
	}

	@Override
	public void abortIfNotComplete() {
	}

	@Override
	public void abort() {
	}

	@Override
	public synchronized void ck_complete() {
		String s = complete();
		if (s != null)
			throw new RuntimeException("transaction commit failed: " + s);
	}

	@Override
	public String complete() {
		return null;
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
				: new Record((ByteBuffer) ref);
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

}
