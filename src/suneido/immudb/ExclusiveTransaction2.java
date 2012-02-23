/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;
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
public class ExclusiveTransaction2 extends UpdateTransaction2
		implements ImmuExclTran {
	private final UpdateDbInfo udbinfo;
	private Tables newSchema;

	ExclusiveTransaction2(int num, Database2 db) {
		super(num, db);
		udbinfo = new UpdateDbInfo(dbstate.dbinfo);
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
	public void addRecord(int tblnum, Record rec) {
		rec.tblnum = tblnum;
		rec.address = rec.store(tran.stor);
		super.addRecord(tblnum, rec);
	}

	@Override
	public void updateRecord(int tblnum, Record from, Record to) {
		to.tblnum = tblnum;
		to.address = to.store(tran.stor);
		super.updateRecord(tblnum, from, to);
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
		int adr = rec.store(tran.stor);
		udbinfo.updateRowInfo(tblnum, 1, rec.bufSize());
		return adr;
	}

	@Override
	protected void storeData() {
		// not required since we are storing as we go
		// just output an empty deletes section
		ByteBuffer buf = tran.stor.buffer(tran.stor.alloc(Shorts.BYTES + Ints.BYTES));
		buf.putShort((short) 0xffff); // mark start of removes
		buf.putInt(0);

	}

	@Override
	protected void updateBtrees(ImmuReadTran t) {
		// no update required since we are updating master directly
		// just need to freeze
		for (Entry<Index, TranIndex> e : indexes.entrySet())
			((Btree2) e.getValue()).freeze();
	}

	@Override
	protected void updateDbInfo(ReadTransaction2 t) {
		udbinfo.dbinfo().freeze();
		db.setState(udbinfo.dbinfo(), newSchema);
	}

	@Override
	public String toString() {
		return "et" + num;
	}

}
