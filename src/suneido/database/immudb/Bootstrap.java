/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

import suneido.database.immudb.schema.*;

/**
 * Create a new database.
 * Tricky because indexes records point to btrees
 * and btrees point to indexes records.
 */
public class Bootstrap {
	static class TN
		{ final static int TABLES = 1, COLUMNS = 2, INDEXES = 3, VIEWS = 4; }
	private static ForeignKey noFkey = new ForeignKey("", "", 0);
	public final Storage stor;
	public final Tran tran;
	private Btree tablesIndex;
	private Btree columnsIndex;
	private Btree indexesIndex;
	private int tablesIndexAdr;
	private int columnsIndexAdr;
	private int indexesIndexAdr;

	public Bootstrap(Storage stor) {
		this.stor = stor;
		tran = new Tran(stor);
	}

	public void create() {
		tablesIndex = new Btree(tran);
		columnsIndex = new Btree(tran);
		indexesIndex = new Btree(tran);

		outputTables();
		outputColumns();
		outputIndexes();

		tran.startStore();
		storeData();
		tablesIndex.store();
		columnsIndex.store();
		indexesIndex.store();
		// TODO update indexes records to convert roots from intref's to adr's
		updateIndexesRecs();
		int redirs = tran.storeRedirs();

		store(indexesIndexAdr, redirs);

		tran.endStore();
	}

	private void outputTables() {
		IndexedData id = new IndexedData().index(tablesIndex, 0);
		addTable(id, TN.TABLES, "tables", 5, 3, 176);
		addTable(id, TN.COLUMNS, "columns", 3, 17, 390);
		addTable(id, TN.INDEXES, "indexes", 9, 5, 331);
	}

	private void addTable(IndexedData id, int tblnum, String name,
			int nextfield, int nrows, long totalsize) {
		Record r = Table.toRecord(tblnum, name, nextfield, nrows, totalsize);
		id.add(tran, r);
	}

	private void outputColumns() {
		IndexedData id = new IndexedData().index(columnsIndex, 0, 2);
		addColumn(id, TN.TABLES, "table", 0);
		addColumn(id, TN.TABLES, "tablename", 1);
		addColumn(id, TN.TABLES, "nextfield", 2);
		addColumn(id, TN.TABLES, "nrows", 3);
		addColumn(id, TN.TABLES, "totalsize", 4);
		addColumn(id, TN.COLUMNS, "table", 0);
		addColumn(id, TN.COLUMNS, "column", 1);
		addColumn(id, TN.COLUMNS, "field", 2);
		addColumn(id, TN.INDEXES, "table", 0);
		addColumn(id, TN.INDEXES, "columns", 1);
		addColumn(id, TN.INDEXES, "key", 2);
		addColumn(id, TN.INDEXES, "fktable", 3);
		addColumn(id, TN.INDEXES, "fkcolumns", 4);
		addColumn(id, TN.INDEXES, "fkmode", 5);
		addColumn(id, TN.INDEXES, "root", 6);
		addColumn(id, TN.INDEXES, "treelevels", 7);
		addColumn(id, TN.INDEXES, "nnodes", 8);
	}

	private void addColumn(IndexedData id, int tblnum, String name, int colnum) {
		Record r = Column.toRecord(tblnum, name, colnum);
		id.add(tran, r);
	}

	private void outputIndexes() {
		IndexedData id = new IndexedData().index(indexesIndex, 0, 1);
		tablesIndexAdr = addIndex(id, TN.TABLES, "table", tablesIndex.info());
		columnsIndexAdr = addIndex(id, TN.COLUMNS, "table,column", columnsIndex.info());
		indexesIndexAdr = addIndex(id, TN.INDEXES, "table,columns", indexesIndex.info());
	}

	private int addIndex(IndexedData id, int tblnum, String columns, BtreeInfo info) {
		Record r = Index.toRecord(tblnum, columns, true, false, noFkey, info);
		return id.add(tran, r);
	}

	private void storeData() {
		IntRefs intrefs = tran.context.intrefs;
		int i = -1;
		for (Object x : intrefs) {
			++i;
			if (x instanceof Record) {
				Record r = (Record) x;
				int adr = r.store(tran.context.stor);
				int intref = i | IntRefs.MASK;
				tran.setAdr(intref, adr);
			}
		}
	}

	private void updateIndexesRecs() {
		updateIndexesRec(tablesIndexAdr, TN.TABLES, "table", tablesIndex.info());
		updateIndexesRec(columnsIndexAdr, TN.COLUMNS, "table,column", columnsIndex.info());
		updateIndexesRec(indexesIndexAdr, TN.INDEXES, "table,columns", indexesIndex.info());
	}

	private void updateIndexesRec(int adr,
			int tblnum, String columns, BtreeInfo info) {
		Record r = Index.toRecord(tblnum, columns, true, false, noFkey, info);
		ByteBuffer buf = stor.buffer(tran.getAdr(adr));
		r.pack(buf); // assumes size hasn't increased
	}

	static final int INT_SIZE = 4;

	private void store(int root, int redirs) {
		root = tran.getAdr(root);
		ByteBuffer buf = stor.buffer(stor.alloc(2 * INT_SIZE));
		buf.putInt(root);
		buf.putInt(redirs);
System.out.println("store root " + root + " redirs " + redirs);
	}

}
