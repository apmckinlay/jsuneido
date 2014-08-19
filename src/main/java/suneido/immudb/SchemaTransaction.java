/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

/**
 * Used by {@link TableBuilder} to make schema changes.
 * Starts out non-exclusive but upgraded to exclusive to add index.
 */
class SchemaTransaction extends UpdateTransaction {

	SchemaTransaction(int num, Database db) {
		super(num, db);
	}

	/** upgrade to exclusive, used by TableBuilder for adding index */
	void exclusive() {
		assert ! ended;
		trans.setExclusive(this);
	}

	/** allow modifying system tables */
	@Override
	protected void checkNotSystemTable(int tblnum, String op) {
	}

	void addTableInfo(TableInfo ti) {
		assert ! ended;
		dbinfo = dbinfo.with(ti);
	}

	void addIndex(Index index) {
		assert ! ended;
		if (hasIndex(index.tblnum, index.colNums))
			return; // bootstrap
		Btree btree = new Btree(tran);
		indexes.put(index, btree);
	}

	void updateTableSchema(Table tbl) {
		assert ! ended;
		Table oldTbl = getTable(tbl.num);
		if (oldTbl != null)
			schema = schema.without(oldTbl);
		schema = schema.with(tbl);
		indexedData.remove(tbl.num);
	}

	void dropTable(Table tbl) {
		assert ! ended;
		// "remove" from dbinfo so indexes won't be persisted
		dbinfo = dbinfo.with(TableInfo.empty(tbl.num));
		schema = schema.without(tbl);
		indexedData.remove(tbl.num);
	}

	//--------------------------------------------------------------------------

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
