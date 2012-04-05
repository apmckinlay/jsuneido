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
	protected Tran.StoreInfo storeInfo = null;

	BulkTransaction(int num, Database db) {
		super(num, db);
		tran.allowStore();
		ByteBuffer buf = tran.dstor.buffer(tran.dstor.alloc(1));
		buf.put((byte) 'e');
onlyReads = false; //TODO remove this once we handle aborts
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
	public int addRecord(int tblnum, Record rec) {
		throw new UnsupportedOperationException("BulkTransaction addRecord");
	}

	@Override
	public int updateRecord(int tblnum, Record from, Record to) {
		throw new UnsupportedOperationException("BulkTransaction updateRecord");
	}

	@Override
	public int removeRecord(int tblnum, Record rec) {
		throw new UnsupportedOperationException("BulkTransaction removeRecord");
	}

	// used by DbLoad and DbCompact
	// doesn't use addRecord because we don't want to update indexes
	int loadRecord(int tblnum, Record rec) {
		rec.tblnum = tblnum;
		rec.address = rec.store(tran.dstor);
		updateRowInfo(tblnum, 1, rec.bufSize());
		return rec.address;
	}

	/** called after creating each btree to save and persist it */
	void saveBtrees() {
		if (storeInfo == null) {
			commitData();
		} else {
			freezeBtrees();
			updateDbInfo();
		}
		db.persist();
		dbstate = db.state;
		dbinfo = dbstate.dbinfo;
		tidelta.clear();
	}

	StoredRecordIterator storedRecordIterator(int first, int last) {
		return new StoredRecordIterator(tran.dstor, first, last);
	}

	@Override
	protected void commit() {
		if (storeInfo == null)
			commitData();
		trans.commit(this);
	}

	private void commitData() {
		endData();
		freezeBtrees();
		updateDbInfo();
	}

	private void endData() {
		// we are storing as we go, so just output the end marker
		UpdateTransaction.endOfCommit(tran.dstor);
		storeInfo = tran.endStore();
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

	private void updateDbInfo() {
		updateDbInfo(indexes);
		db.setState(dbinfo, schema, storeInfo.cksum, storeInfo.adr);
	}

	@Override
	public void abort() {
		if (storeInfo == null)
			endData();
		Database.State state = db.state;
		db.setState(state.dbinfo, state.schema, storeInfo.cksum, storeInfo.adr);
		//TODO prevent output from being seen by rebuild
		super.abort();
	}

	@Override
	public String toString() {
		return "bt" + num;
	}

}
