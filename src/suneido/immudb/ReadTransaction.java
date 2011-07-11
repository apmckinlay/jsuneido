/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import javax.annotation.concurrent.NotThreadSafe;

import suneido.immudb.schema.Table;
import suneido.immudb.schema.Tables;

import com.google.common.collect.HashBasedTable;
import com.google.common.primitives.Ints;

/**
 * Transactions must be thread contained.
 * They take a "snapshot" of the database state at the start.
 * ReadTransactions require no locking
 * since they only operate on immutable data.
 */
@NotThreadSafe
public class ReadTransaction {
	protected final Storage stor;
	protected final Tran tran;
	protected final DbInfo dbinfo;
	protected Tables schema;
	protected final HashBasedTable<Integer,String,Btree> indexes = HashBasedTable.create();

	public ReadTransaction(Database db) {
		this(db.stor, db.dbinfo, db.schema, db.redirs);
	}

	public ReadTransaction(Storage stor, DbHashTrie dbinfo, Tables schema, DbHashTrie redirs) {
		this.stor = stor;
		this.dbinfo = new DbInfo(stor, dbinfo);
		this.schema = schema;
		tran = new Tran(stor, new Redirects(redirs));
	}

	public Btree getIndex(int tblnum, int... indexColumns) {
		return getIndex(tblnum, Ints.join(",", indexColumns));
	}

	/** indexColumns are like "3,4" */
	public Btree getIndex(int tblnum, String indexColumns) {
		Btree btree = indexes.get(tblnum, indexColumns);
		if (btree != null)
			return btree;
		TableInfo ti = dbinfo.get(tblnum);
		btree = new Btree(tran, ti.getIndex(indexColumns));
		indexes.put(tblnum, indexColumns, btree);
		return btree;
	}

	public boolean hasIndex(int tblnum, String indexColumns) {
		return indexes.contains(tblnum, indexColumns);
	}

	public Record getrec(int adr) {
		return tran.getrec(adr);
	}

	public Record lookup(int tblnum, String indexColumns, Record key) {
		Btree btree = getIndex(tblnum, indexColumns);
		int adr = btree.get(key);
		if (adr == 0)
			return null; // not found
		return getrec(adr);
	}

	public Table getTable(String tableName) {
		return schema.get(tableName);
	}

	public Table getTable(int tblnum) {
		return schema.get(tblnum);
	}

	public TableInfo getTableInfo(int tblnum) {
		return dbinfo.get(tblnum);
	}

	/** @return view definition, else null if view not found */
	public String getView(String name) {
		return Views.getView(this, name);
	}

}
