/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb.schema;

import java.nio.ByteBuffer;

import suneido.database.immudb.*;

import com.google.common.collect.ImmutableList;

/**
 * Create a new database with the initial schema:
 * 	tables (table, tablename)
 * 		key(table)
 * 	columns (table, column, field)
 * 		key(table, field)
 * 	indexes (table,columns,key,fktable,fkcolumns,fkmode)
 *		key(table,columns)
 */
public class Bootstrap {
	public static class TN
		{ public final static int TABLES = 1, COLUMNS = 2, INDEXES = 3, VIEWS = 4; }
	private static ForeignKey noFkey = new ForeignKey("", "", 0);
	public final Storage stor;
	public final Tran tran;
	private Btree tablesIndex;
	private Btree columnsIndex;
	private Btree indexesIndex;

	public Bootstrap(Storage stor) {
		this.stor = stor;
		tran = new Tran(stor);
	}

	public void create() {
		outputTables();
		outputColumns();
		outputIndexes();

		tran.startStore();
		DataRecords.store(tran);
		Btree.store(tran);
		int dbinfo = createInfo();
		int redirs = tran.storeRedirs();
		store(dbinfo, redirs);
		tran.endStore();
	}

	private void outputTables() {
		tablesIndex = new Btree(tran);
		IndexedData id = new IndexedData().index(tablesIndex, 0);
		addTable(id, TN.TABLES, "tables");
		addTable(id, TN.COLUMNS, "columns");
		addTable(id, TN.INDEXES, "indexes");
	}
	private void addTable(IndexedData id, int tblnum, String name) {
		Record r = Table.toRecord(tblnum, name);
		id.add(tran, r);
	}

	private void outputColumns() {
		columnsIndex = new Btree(tran);
		IndexedData id = new IndexedData().index(columnsIndex, 0, 1);
		addColumn(id, TN.TABLES, "table", 0);
		addColumn(id, TN.TABLES, "tablename", 1);

		addColumn(id, TN.COLUMNS, "table", 0);
		addColumn(id, TN.COLUMNS, "field", 1);
		addColumn(id, TN.COLUMNS, "column", 2);

		addColumn(id, TN.INDEXES, "table", 0);
		addColumn(id, TN.INDEXES, "columns", 1);
		addColumn(id, TN.INDEXES, "key", 2);
		addColumn(id, TN.INDEXES, "fktable", 3);
		addColumn(id, TN.INDEXES, "fkcolumns", 4);
		addColumn(id, TN.INDEXES, "fkmode", 5);
	}
	private void addColumn(IndexedData id, int tblnum, String name, int colnum) {
		Record r = Column.toRecord(tblnum, colnum, name);
		id.add(tran, r);
	}

	private void outputIndexes() {
		indexesIndex = new Btree(tran);
		IndexedData id = new IndexedData().index(indexesIndex, 0, 1);
		addIndex(id, TN.TABLES, "0");
		addIndex(id, TN.COLUMNS, "0,1");
		addIndex(id, TN.INDEXES, "0,1");
	}
	private int addIndex(IndexedData id, int tblnum, String columns) {
		Record r = Index.toRecord(tblnum, columns, true, false, noFkey);
		return id.add(tran, r);
	}

	private int createInfo() {
		DbInfo dbinfo = new DbInfo(tran);
		dbinfo.add(new TableInfo(TN.TABLES, 2, 3, 176,
				ImmutableList.of(new IndexInfo("0", tablesIndex.info()))));
		dbinfo.add(new TableInfo(TN.COLUMNS, 3, 11, 390,
				ImmutableList.of(new IndexInfo("0,1", columnsIndex.info()))));
		dbinfo.add(new TableInfo(TN.INDEXES, 6, 3, 331,
				ImmutableList.of(new IndexInfo("0,1", indexesIndex.info()))));
		return dbinfo.store();
	}

	static final int INT_SIZE = 4;

	private void store(int root, int redirs) {
		ByteBuffer buf = stor.buffer(stor.alloc(2 * INT_SIZE));
		buf.putInt(root);
		buf.putInt(redirs);
	}

}
