/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import suneido.SuException;
import suneido.util.ThreadConfined;

/**
 * Used for schema changes and for load and compact.
 * Changes are made directly to master btrees (no overlay).
 * Load and compact bend the rules and write data prior to commit
 * using loadRecord and saveBtrees
 */
@ThreadConfined
public class ExclusiveTransaction2 extends UpdateTransaction2
		implements ImmuExclTran {
	private final UpdateDbInfo udbinfo;
	private Tables newSchema;

	ExclusiveTransaction2(int num, Database2 db) {
		super(num, db);
		udbinfo = new UpdateDbInfo(stor, dbstate.dbinfo);
		newSchema = schema;
		tran.allowStore();
	}

	@Override
	protected void lock(Database2 db) {
		assert ! db.exclusiveLock.isWriteLocked() : "already exclusively locked";
		if (! db.exclusiveLock.writeLock().tryLock())
			throw new SuException("can't make schema changes " +
					"when there are outstanding update transactions");
		locked = true;
	}

	@Override
	protected void unlock() {
		tran.endStore();
		db.exclusiveLock.writeLock().unlock();
		locked = false;
	}

	@Override
	public TableInfo getTableInfo(int tblnum) {
		return udbinfo.get(tblnum);
	}

	// override UpdateTransaction (back to the same as ReadTransaction)
	@Override
	protected TranIndex getIndex(IndexInfo info) {
		return new Btree2(tran, info);
	}

	@Override
	public Table getTable(String tableName) {
		return newSchema.get(tableName);
	}

	@Override
	public Table getTable(int tblnum) {
		return newSchema.get(tblnum);
	}

	@Override
	void verifyNotSystemTable(int tblnum, String what) {
	}

	@Override
	protected void updateRowInfo(int tblnum, int nrows, int size) {
		udbinfo.updateRowInfo(tblnum, nrows, size);
	}

	// used by Bootstrap and TableBuilder
	@Override
	public Btree2 addIndex(Index index) {
		assert locked;
		Btree2 btree = new Btree2(tran);
		indexes.put(index, btree);
		return btree;
	}

	// used by TableBuilder
	@Override
	public void addSchemaTable(Table tbl) {
		assert locked;
		newSchema = newSchema.with(tbl);
	}

	// used by TableBuilder and Bootstrap
	@Override
	public void addTableInfo(TableInfo ti) {
		assert locked;
		udbinfo.add(ti);
	}

	// used by TableBuilder
	@Override
	public void updateSchemaTable(Table tbl) {
		assert locked;
		Table oldTbl = getTable(tbl.num);
		if (oldTbl != null)
			newSchema = newSchema.without(oldTbl);
		newSchema = newSchema.with(tbl);
	}

	// used by TableBuilder
	@Override
	public void dropTableSchema(Table table) {
		newSchema = newSchema.without(table);
	}

	// used by DbLoad and DbCompact
	int loadRecord(int tblnum, Record rec) {
		rec.tblnum = tblnum;
		int adr = rec.store(stor);
		udbinfo.updateRowInfo(tblnum, 1, rec.bufSize());
		return adr;
	}

	@Override
	protected void storeData() {
		// not required since we are storing as we go
	}

	@Override
	protected void updateBtrees(ImmuReadTran t) {
		// not required since we are updating master directly
	}

	@Override
	protected void updateDbInfo(ReadTransaction2 t) {
		udbinfo.dbinfo().freeze();
		db.setState(new DatabaseState2(udbinfo.dbinfo(), newSchema));
	}

	@Override
	public void abort() {
		try {
//			int redirsAdr = db.getRedirs().store(null);
//			int dbinfoAdr = db.getDbinfo().store(null);
//			store(dbinfoAdr, redirsAdr);
			tran.endStore();
		} finally {
			super.abort();
		}
	}

	@Override
	public String toString() {
		return "et" + num;
	}

}
