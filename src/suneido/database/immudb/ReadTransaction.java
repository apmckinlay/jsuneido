/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import javax.annotation.concurrent.NotThreadSafe;

import suneido.database.immudb.schema.Tables;

import com.google.common.collect.HashBasedTable;
import com.google.common.primitives.Ints;

/**
 * Transactions should be thread contained.
 * They take a "snapshot" of the database state at the start
 * and then update the database state when they commit.
 * Storage is only written during commit.
 * Commit is single-threaded.
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

	public void commit() {
		// nothing to do for read-only transaction
	}

	public Record getrec(int adr) {
		return tran.getrec(adr);
	}

}
