/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.Immutable;

import suneido.SuException;
import suneido.intfc.database.HistoryIterator;
import suneido.intfc.database.IndexIter;

import com.google.common.collect.HashBasedTable;

/**
 * Transactions must be thread contained.
 * They take a "snapshot" of the database state at the start.
 * ReadTransactions require no locking
 * since they only operate on immutable data.
 */
@Immutable
class ReadTransaction implements suneido.intfc.database.Transaction {
	protected final Storage stor;
	protected final Tran tran;
	private final ReadDbInfo dbinfo;
	protected final Tables schema;
	protected final HashBasedTable<Integer,ColNums,Btree> indexes = HashBasedTable.create();
	protected final int num;

	ReadTransaction(int num,
			Storage stor, DbHashTrie dbinfo, Tables schema, DbHashTrie redirs) {
		this.num = num;
		this.stor = stor;
		this.dbinfo = new ReadDbInfo(stor, dbinfo);
		this.schema = schema;
		tran = new Tran(stor, new Redirects(redirs));
	}

	protected DbHashTrie originalDbinfo() {
		return dbinfo.dbinfo;
	}

	protected ReadDbInfo dbinfo() {
		return dbinfo;
	}

	Btree getIndex(int tblnum, String columns) {
		Table tbl = ck_getTable(tblnum);
		int[] fields = tbl.namesToNums(columns);
		return getIndex(tblnum, fields);
	}

	Btree getIndex(int tblnum, int... indexColumns) {
		ColNums colNums = new ColNums(indexColumns);
		Btree btree = indexes.get(tblnum, colNums);
		if (btree != null)
			return btree;
		TableInfo ti = dbinfo().get(tblnum);
		btree = new Btree(tran, ti.getIndex(indexColumns));
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
	public Table getTable(String tableName) {
		return schema.get(tableName);
	}

	@Override
	public Table getTable(int tblnum) {
		return schema.get(tblnum);
	}

	TableInfo getTableInfo(int tblnum) {
		return dbinfo().get(tblnum);
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
	public void ck_complete() {
	}

	@Override
	public String complete() {
		return "";
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
	public suneido.intfc.database.Record lookup(int tblnum, String index,
			suneido.intfc.database.Record key) {
		throw new UnsupportedOperationException(); //TODO
	}

	@Override
	public void callTrigger(suneido.intfc.database.Table table,
			suneido.intfc.database.Record oldrec,
			suneido.intfc.database.Record newrec) {
		throw new UnsupportedOperationException(); //TODO
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

}
