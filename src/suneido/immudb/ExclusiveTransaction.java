/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

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
		dbinfo.add(ti);
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

	// used by DbLoad
	int loadRecord(int tblnum, Record rec, Btree btree, int[] fields) {
		rec.tblnum = tblnum;
		int adr = rec.store(stor);
		Record key = IndexedData.key(rec, fields, adr);
		btree.add(key);
		dbinfo.updateRowInfo(tblnum, 1, rec.bufSize());
		return adr;
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
		int redirsAdr = db.redirs.store(null);
		int dbinfoAdr = db.dbinfo.store(null);
		store(dbinfoAdr, redirsAdr);
		tran.endStore();
		super.abort();
	}

}
