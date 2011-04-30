/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

public class ExclusiveTransaction extends UpdateTransaction {

	ExclusiveTransaction(Database db) {
		super(db);
		tran.allowStore();
	}

	@Override
	protected void lock(Database db) {
		if (! db.exclusiveLock.writeLock().tryLock())
			throw new RuntimeException("ExclusiveTransaction: could not get lock");
	}

	@Override
	protected void unlock() {
		db.exclusiveLock.writeLock().unlock();
	}

	// used by DbLoad
	public int loadRecord(int tblnum, Record rec, Btree btree, int[] fields) {
		int adr = rec.store(stor);
		Record key = IndexedData.key(rec, fields, adr);
		btree.add(key);
		dbinfo.addrow(tblnum, rec.length());
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
