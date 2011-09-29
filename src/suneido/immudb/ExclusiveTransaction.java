/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import suneido.util.ThreadConfined;

/**
 * Transactions must be thread confined.
 * Load and compact bend the rules and write data prior to commit
 * using loadRecord and saveBtrees
 */
@ThreadConfined
public class ExclusiveTransaction extends UpdateTransaction {

	ExclusiveTransaction(int num, Database db) {
		super(num, db);
		tran.allowStore();
	}

	@Override
	protected void lock(Database db) {
		if (! db.exclusiveLock.writeLock().tryLock())
			throw new RuntimeException("ExclusiveTransaction: could not get lock");
		locked = true;
	}

	@Override
	protected void unlock() {
		db.exclusiveLock.writeLock().unlock();
		locked = false;
	}

	// used by TableBuilder
	void addSchemaTable(Table tbl) {
		assert locked;
		newSchema = newSchema.with(this, tbl);
	}

	// used by TableBuilder and Bootstrap
	void addTableInfo(TableInfo ti) {
		assert locked;
		udbinfo.add(ti);
	}

	// used by TableBuilder
	void updateSchemaTable(Table tbl) {
		assert locked;
		Table oldTbl = getTable(tbl.num);
		if (oldTbl != null)
			newSchema = newSchema.without(this, oldTbl);
		newSchema = newSchema.with(this, tbl);
	}

	// used by TableBuilder
	void dropTableSchema(Table table) {
		newSchema = newSchema.without(this, table);
	}

	// used by DbLoad and DbCompact
	int loadRecord(int tblnum, Record rec) {
		rec.tblnum = tblnum;
		int adr = rec.store(stor);
		udbinfo.updateRowInfo(tblnum, 1, rec.bufSize());
		return adr;
	}

	@Override
	protected void mergeDatabaseDbInfo() {
		assert rdbinfo.dbinfo == db.getDbinfo();
	}

	@Override
	protected void mergeRedirs() {
		tran.assertNoRedirChanges(db.getRedirs());
	}

	// used by DbLoad
	void saveBtrees() {
		tran.intrefs.startStore();
		Btree.store(tran);
		for (Btree btree : indexes.values())
			btree.info(); // convert roots from intrefs
		tran.intrefs.clear();
	}

	@Override
	public void abort() {
		try {
			int redirsAdr = db.getRedirs().store(null);
			int dbinfoAdr = db.getDbinfo().store(null);
			store(dbinfoAdr, redirsAdr);
			tran.endStore();
		} finally {
			super.abort();
		}
	}

	@Override
	public void readLock(int adr) {
	}

	@Override
	public void writeLock(int adr) {
	}

	@Override
	public String toString() {
		return "et" + num;
	}

}
