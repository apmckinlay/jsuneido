/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import suneido.SuException;
import suneido.immudb.TranIndex.Iter;

class SchemaTransaction extends UpdateTransaction {

	SchemaTransaction(int num, Database db) {
		super(num, db);
	}

	@Override
	protected void lock(Database db) {
		assert ! db.exclusiveLock.isWriteLocked() : "already exclusively locked";
		if (! db.exclusiveLock.writeLock().tryLock())
			throw new SuException("can't make schema changes " +
					"when there are outstanding update transactions");
		locked = true;
	}

	@Override
	protected void unlock() {
		db.exclusiveLock.writeLock().unlock();
		locked = false;
	}

	@Override
	void verifyNotSystemTable(int tblnum, String what) {
	}

	void addTableInfo(TableInfo ti) {
		assert locked;
		dbinfo = dbinfo.with(ti);
	}

	Btree addIndex(Index index) {
		assert locked;
		Btree btree = new Btree(tran);
		indexes.put(index, btree);
		return btree;
	}

	void addTableSchema(Table tbl) {
		assert locked;
		schema = schema.with(tbl);
		indexedData.remove(tbl.num);
	}

	void updateTableSchema(Table tbl) {
		assert locked;
		Table oldTbl = getTable(tbl.num);
		if (oldTbl != null)
			schema = schema.without(oldTbl);
		schema = schema.with(tbl);
		indexedData.remove(tbl.num);
	}

	void dropTable(Table tbl) {
		// "remove" from dbinfo so indexes won't be persisted
		dbinfo = dbinfo.with(TableInfo.empty(tbl.num));
		schema = schema.without(tbl);
		indexedData.remove(tbl.num);
	}

	//--------------------------------------------------------------------------

	// don't need to track reads when exclusive
	@Override
	protected void trackReads(Index index, Iter iter) {
	}

	// don't need to check for conflicts when exclusive
	@Override
	protected void checkForConflicts() {
	}

	@Override
	protected Btree getLatestIndex(Index index) {
		// if index was added in this tran, then no master, so create one
		return (indexes.get(index) instanceof Btree)
				? new Btree(tran) : super.getLatestIndex(index);
	}

	@Override
	protected char tranType() {
		return 's';
	}

	@Override
	protected void updateDbInfo() {
		updateDbInfo(updatedIndexes);
	}

	@Override
	public String toString() {
		return "st" + num;
	}

}
