/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;


public class ExclusiveTransaction extends UpdateTransaction {

	ExclusiveTransaction(Database db) {
		super(db);
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

	public void dropTableSchema(Table table) {
		schema = schema.without(table);
	}

	// used by DbLoad
	public int loadRecord(int tblnum, Record rec, Btree btree, int[] fields) {
		int adr = rec.store(stor);
		Record key = IndexedData.key(rec, fields, adr);
		btree.add(key);
		dbinfo.updateRowInfo(tblnum, 1, rec.bufSize());
		return adr;
	}

	// used by DbLoad
	public void saveBtrees() {
		tran.intrefs.startStore();
		Btree.store(tran);
		for (Btree btree : indexes.values())
			btree.info(); // convert roots from intrefs
		tran.intrefs.clear();
	}

}
