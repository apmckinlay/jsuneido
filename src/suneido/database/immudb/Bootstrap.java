/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

import suneido.database.immudb.schema.*;

/**
 * create a brand new database
 */
public class Bootstrap {
	static class TN {
		final static int TABLES = 1, COLUMNS = 2, INDEXES = 3, VIEWS = 4; }
	private static ForeignKey noFkey = new ForeignKey("", "", 0);
	public final Storage stor;
	public final Tran tran;
	private Btree tables1;
	private Btree tables2;
	private Btree columns1;
	private Btree indexes1;
	private Btree indexes2;

	public Bootstrap(Storage stor) {
		this.stor = stor;
		tran = new Tran(stor);
	}

	public void create() {
		tables1 = new Btree(tran);
		tables2 = new Btree(tran);
		columns1 = new Btree(tran);
		indexes1 = new Btree(tran);
		indexes2 = new Btree(tran);

		outputTables();
		outputColumns();
		int root = outputIndexes();

		tran.startStore();
		storeData();
		tables1.store();
		tables2.store();
		columns1.store();
		indexes2.store();
		indexes1.store();
		// TODO update indexes records to convert roots from intref's to adr's
		int redirs = tran.storeRedirs();

		store(root, redirs);

		tran.endStore();
	}

	private void outputTables() {
		IndexedData id = new IndexedData().index(tables1, 0).index(tables2, 1);
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
		IndexedData id = new IndexedData().index(columns1, 0, 1);
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

	private void addColumn(IndexedData id, int tblnum, String name, int num) {
		Record r = Column.toRecord(tblnum, name, num);
		id.add(tran, r);
	}

	/** @return The intref of the indexes1 record */
	private int outputIndexes() {
		IndexedData id = new IndexedData().index(indexes1, 0, 1).index(indexes2, 3, 4);
		addIndex(id, TN.TABLES, "table", true, tables1.info());
		addIndex(id, TN.TABLES, "tablename", true, tables2.info());
		addIndex(id, TN.COLUMNS, "table,column", true, columns1.info());
		addIndex(id, TN.INDEXES, "fktable,fkcolumns", false, indexes2.info());
		int ref = addIndex(id, TN.INDEXES, "table,columns", true, indexes1.info());
		// update record to include itself
		Record r = Index.toRecord(TN.INDEXES, "table,columns",
				true, false, noFkey, indexes1.info());
		tran.redir(ref, r);
		return ref;
	}

	private int addIndex(IndexedData id, int tblnum, String columns, boolean key,
			BtreeInfo info) {
		Record r = Index.toRecord(tblnum, columns, key, false, noFkey, info);
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

	static final int INT_SIZE = 4;

	private void store(int root, int redirs) {
		root = tran.getAdr(root);
		ByteBuffer buf = stor.buffer(stor.alloc(2 * INT_SIZE));
		buf.putInt(root);
		buf.putInt(redirs);
System.out.println("store root " + root + " redirs " + redirs);
	}

}
