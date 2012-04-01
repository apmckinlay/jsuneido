/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map.Entry;

import suneido.SuException;
import suneido.util.ThreadConfined;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Shorts;

/**
 * Used for schema changes and for load and compact.
 * Changes are made directly to master btrees (no overlay).
 * Load and compact bend the rules and write data prior to commit
 * using loadRecord and saveBtrees
 */
@ThreadConfined
class ExclusiveTransaction extends ReadWriteTransaction {
	private boolean data_committed = false;
	private Tran.StoreInfo storeInfo = null;

	ExclusiveTransaction(int num, Database db) {
		super(num, db);
		tran.allowStore();
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
	public void addRecord(int tblnum, Record rec) {
		rec.tblnum = tblnum;
		rec.address = rec.store(tran.dstor);
		super.addRecord(tblnum, rec);
	}

	@Override
	public int updateRecord(int tblnum, Record from, Record to) {
		to.tblnum = tblnum;
		to.address = to.store(tran.dstor);
		return super.updateRecord(tblnum, from, to);
	}

	@Override
	void verifyNotSystemTable(int tblnum, String what) {
	}

	// used by Bootstrap and TableBuilder
	Btree addIndex(Index index) {
		assert locked;
		Btree btree = new Btree(tran);
		indexes.put(index, btree);
		return btree;
	}

	// used by TableBuilder
	void addSchemaTable(Table tbl) {
		assert locked;
		schema = schema.with(tbl);
		indexedData.remove(tbl.num);
	}

	// used by TableBuilder and Bootstrap
	void addTableInfo(TableInfo ti) {
		assert locked;
		dbinfo = dbinfo.with(ti);
	}

	// used by TableBuilder
	void updateSchemaTable(Table tbl) {
		assert locked;
		Table oldTbl = getTable(tbl.num);
		if (oldTbl != null)
			schema = schema.without(oldTbl);
		schema = schema.with(tbl);
		indexedData.remove(tbl.num);
	}

	// used by TableBuilder
	void dropTableSchema(Table table) {
		// "remove" from dbinfo so indexes won't be persisted
		dbinfo = dbinfo.with(TableInfo.empty(table.num));
		schema = schema.without(table);
		indexedData.remove(table.num);
	}

	// used by DbLoad and DbCompact
	// doesn't use addRecord because we don't want to update indexes
	int loadRecord(int tblnum, Record rec) {
		rec.tblnum = tblnum;
		rec.address = rec.store(tran.dstor);
		updateRowInfo(tblnum, 1, rec.bufSize());
		return rec.address;
	}

	// used by DbLoad to do incremental commit
	void saveBtrees() {
		if (! data_committed) {
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
		if (! data_committed)
			commitData();
		trans.commit(this);
	}

	private void commitData() {
		storeData();
		freezeBtrees();
		updateDbInfo();
		data_committed = true;
	}

	private void storeData() {
		// not required since we are storing as we go
		// just output an empty deletes section
		ByteBuffer buf = tran.dstor.buffer(tran.dstor.alloc(Shorts.BYTES + Ints.BYTES));
		buf.putShort((short) 0xffff); // mark start of removes
		buf.putInt(0); // mark end of removes
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
		if (! data_committed)
			storeData();
		Database.State state = db.state;
		db.setState(state.dbinfo, state.schema, storeInfo.cksum, storeInfo.adr);
		// else
			//TODO prevent output from being seen by rebuild
		super.abort();
	}

	@Override
	public String toString() {
		return "et" + num;
	}

}
