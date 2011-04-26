/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;
import java.util.Map;

import suneido.database.immudb.schema.Table;
import suneido.database.immudb.schema.Tables;

import com.google.common.collect.ImmutableList;

public class UpdateTransaction extends ReadTransaction {
	private final Database db;
	private final Tables original_schema;

	public UpdateTransaction(Database db) {
		super(db);
		this.db = db;
		original_schema = db.schema;
	}

	public void addIndex(int tblnum, String indexColumns) {
		indexes.put(tblnum, indexColumns, new Btree(tran));
	}

	public void addSchemaTable(Table table) {
		schema = schema.with(table);
	}

	public void addTableInfo(TableInfo ti) {
		dbinfo.add(ti);
	}

	// used by TableBuilder
	public void addRecord(Record r, int tblnum, int... indexColumns) {
		Btree btree = getIndex(tblnum, indexColumns);
		IndexedData id = new IndexedData().index(btree, indexColumns);
		id.add(tran, r);
	}

	// TODO synchronize
	@Override
	public void commit() {
		tran.startStore();
		DataRecords.store(tran);
		Btree.store(tran);

		updateDbInfo();
		// TODO update nrows, totalsize

		int redirs = tran.storeRedirs();
		store(dbinfo.store(), redirs);
		tran.endStore();

		updateSchema();

		// TODO merge dbinfo and redirs with database
	}

	private void updateDbInfo() {
		for (int tblnum : indexes.rowKeySet()) {
			TableInfo ti = dbinfo.get(tblnum);
			Map<String,Btree> idxs = indexes.row(tblnum);
			ImmutableList.Builder<IndexInfo> b = ImmutableList.builder();
			for (IndexInfo ii : ti.indexInfo) {
				Btree btree = idxs.get(ii.columns);
				b.add((btree == null)
						? ii : new IndexInfo(ii.columns, btree.info()));
			}
			ti = new TableInfo(tblnum, ti.nextfield, ti.nrows, ti.totalsize,
					b.build());
			dbinfo.add(ti);
		}
	}

	static final int INT_SIZE = 4;

	private void store(int dbinfo, int redirs) {
		ByteBuffer buf = stor.buffer(stor.alloc(2 * INT_SIZE));
		buf.putInt(dbinfo);
		buf.putInt(redirs);
	}

	private void updateSchema() {
		if (schema == original_schema)
			return; // no schema changes in this transaction

		// schema changes must be done while holding the commit lock
		// so there should be no concurrent changes
		assert db.schema == original_schema;

		db.schema = schema;
	}

}
