/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map.Entry;

import suneido.SuException;
import suneido.util.ThreadConfined;

/**
 * Used for load and compact.
 * Changes are made directly to master btrees (no overlay).
 * Unlike other transactions, data is written prior to commit (not deferred)
 * using loadRecord and saveBtrees
 */
@ThreadConfined
class BulkTransaction extends ReadWriteTransaction {
	private boolean storeStarted = false;
	private Persist persist = null;

	BulkTransaction(int num, Database db) {
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

	// used by DbLoad and DbCompact
	// doesn't use addRecord because we don't want to update indexes yet
	int loadRecord(int tblnum, Record rec) {
		ensureStore();
		rec.tblnum = tblnum;
		rec.address = rec.store(tran.dstor);
		updateRowInfo(tblnum, 1, rec.bufSize());
		onlyReads = false;
		return rec.address;
	}

	protected void ensureStore() {
		if (storeStarted)
			return;
		tran.allowStore();
		ByteBuffer buf = tran.dstor.buffer(tran.dstor.alloc(1));
		buf.put((byte) 'b');
		storeStarted = true;
	}

	@Override
	public int addRecord(int tblnum, Record rec) {
		throw new UnsupportedOperationException("BulkTransaction addRecord");
	}

	@Override
	public int updateRecord2(int tblnum, Record from, Record to) {
		throw new UnsupportedOperationException("BulkTransaction updateRecord");
	}

	@Override
	public int removeRecord(int tblnum, Record rec) {
		throw new UnsupportedOperationException("BulkTransaction removeRecord");
	}

	/** called after creating each btree to persist it */
	void saveBtrees() {
		ensurePersist();
		freezeBtrees();
		updateDbInfo(indexes);
		persist.storeBtrees(dbinfo);
		tidelta.clear();
	}

	private void ensurePersist() {
		if (persist != null)
			return;
		persist = new Persist(dbinfo, db.istor);
		persist.startStore();
	}

	private void freezeBtrees() {
		// no actual update required since we are updating master directly
		Iterator<Entry<Index, TranIndex>> iter = indexes.entrySet().iterator();
		while (iter.hasNext()) {
			Btree btree = (Btree) iter.next().getValue();
			if (btree.frozen())
				iter.remove(); // since we don't need to update dbinfo
			else
				btree.freeze();
		}
	}

	/** used by DbLoad createIndexes to iterate through data */
	StoredRecordIterator storedRecordIterator(int first, int last) {
		return new StoredRecordIterator(tran.dstor, first, last);
	}

	@Override
	protected void commit() {
		Tran.StoreInfo info = endDataStore();
		persist.finish(db, schema, info.cksum, info.adr);
		trans.commit(this);
	}

	private Tran.StoreInfo endDataStore() {
		// we are storing as we go, so just output the end marker
		UpdateTransaction.endOfCommit(tran.dstor);
		return tran.endStore();
	}

	@Override
	public void abort() {
		if (storeStarted) {
			UpdateTransaction.endOfCommit(tran.dstor); // so dump works
			tran.abortIncompleteStore();
		}
		if (persist != null)
			persist.abort(db.state);
		super.abort();
	}

	@Override
	public String toString() {
		return "bt" + num;
	}

}
