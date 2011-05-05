/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.util.*;

import suneido.database.immudb.Bootstrap.TN;
import suneido.database.immudb.schema.*;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;

public class TableBuilder {
	public static final String COLUMN_IN_INDEX = "column used in index";
	public static final String TABLE_MUST_HAVE_KEY = "tables must have at least one key";
	public static final String NONEXISTENT_TABLE = "nonexistent table";
	public static final String NONEXISTENT_COLUMN = "nonexistent column";
	public static final String NONEXISTENT_INDEX = "nonexistent index";
	private static final String CANT_DROP = "can't drop ";
	private static final String CANT_RENAME = "can't rename ";
	public static final String TO_EXISTING = "to existing column";
	private final String tableName;
	private final int tblnum;
	private final UpdateTransaction t;
	private final List<Column> columns = Lists.newArrayList();
	private final List<Index> indexes = Lists.newArrayList();

	public static TableBuilder create(UpdateTransaction t, String tablename, int tblnum) {
		TableBuilder tb = new TableBuilder(t, tablename, tblnum);
		tb.createTable();
		return tb;
	}

	public static TableBuilder alter(UpdateTransaction t, String tableName) {
		Table table = t.getTable(tableName);
		if (table == null)
			fail(t, NONEXISTENT_TABLE + ": " + tableName);
		TableBuilder tb = new TableBuilder(t, tableName, table.num);
		tb.getSchema();
		return tb;
	}

	private TableBuilder(UpdateTransaction t, String tblname, int tblnum) {
		this.t = t;
		this.tableName = tblname;
		this.tblnum = tblnum;
	}

	private void createTable() {
		t.addRecord(TN.TABLES, Table.toRecord(tblnum, tableName));
	}

	public static void dropTable(ExclusiveTransaction t, String tableName) {
		// TODO drop view
		Table table = t.getTable(tableName);
		if (table == null)
			fail(t, CANT_DROP + NONEXISTENT_TABLE + ": " + tableName);
		t.removeRecord(TN.TABLES, table.toRecord());
		t.dropTableSchema(table);
		// NOTE: leaves dbinfo (DbHashTrie doesn't have remove)
		t.commit();
	}

	public static void renameTable(ExclusiveTransaction t, String from, String to) {
		Table oldTable = t.getTable(from);
		if (oldTable == null)
			fail(t, CANT_RENAME + NONEXISTENT_TABLE + ": " + from);
		t.dropTableSchema(oldTable);
		Table newTable = new Table(oldTable.num, to,
				new Columns(ImmutableList.copyOf(oldTable.columns)),
				new Indexes(ImmutableList.copyOf(oldTable.indexes)));
		t.addSchemaTable(newTable);
		// dbinfo is ok since it doesn't use table name
		t.updateRecord(TN.TABLES, oldTable.toRecord(), newTable.toRecord());
		t.commit();
	}

	private void getSchema() {
		Table table = t.getTable(tableName);
		columns.addAll(table.columnsList());
		indexes.addAll(table.indexesList());
	}

	public void ensureColumn(String column) {
		if (! hasColumn(column))
			addColumn(column);
	}

	public void addColumn(String column) {
		int field = nextColNum();
		Column c = new Column(tblnum, field, column);
		t.addRecord(TN.COLUMNS, c.toRecord());
		columns.add(c);
	}

	private int nextColNum() {
		int maxNum = -1;
		for (Column c : columns)
			if (c.field > maxNum)
				maxNum = c.field;
		return maxNum + 1;
	}

	public void renameColumn(String from, String to) {
		if (hasColumn(to))
			fail(CANT_RENAME + TO_EXISTING + ": " + to);
		int i = findColumn(from);
		if (i == -1)
			fail(CANT_RENAME + NONEXISTENT_COLUMN + ": " + from);
		Column c = columns.get(i);
		Record oldRec = c.toRecord();
		c = new Column(tblnum, c.field, to);
		columns.set(i, c);
		Record newRec = c.toRecord();
		t.updateRecord(TN.COLUMNS, oldRec, newRec);
		// don't need to update indexes because they use column numbers not names
	}

	public void dropColumn(String column) {
		int i = findColumn(column);
		if (i == -1)
			fail(CANT_DROP + NONEXISTENT_COLUMN + ": " + column);
		Column c = columns.get(i);
		mustNotBeUsedByIndex(column);
		t.removeRecord(TN.COLUMNS, c.toRecord());
		columns.remove(i);
	}

	private int findColumn(String column) {
		for (int i = 0; i < columns.size(); ++i) {
			Column c = columns.get(i);
			if (c.name.equals(column))
				return i;
		}
		return -1;
	}

	private void mustNotBeUsedByIndex(String column) {
		int colNum = colNum(column);
		for (int i = 0; i < indexes.size(); ++i) {
			Index index = indexes.get(i);
			if (Ints.contains(index.colNums, colNum))
				fail(CANT_DROP + COLUMN_IN_INDEX + ": " + column);
		}
	}

	public void ensureIndex(String columnNames, boolean isKey, boolean unique,
			String fktable, String fkcolumns, int fkmode) {
		if (! hasIndex(columnNames))
			addIndex(columnNames, isKey, unique, fktable, fkcolumns, fkmode);
	}

	public void addIndex(String columnNames, boolean isKey, boolean unique,
			String fktable, String fkcolumns, int fkmode) {
		int[] colNums = colNums(columnNames);
		Index index = new Index(tblnum, colNums, isKey, unique);
		t.addRecord(TN.INDEXES, index.toRecord());
		indexes.add(index);
		String colNumsStr = Ints.join(",", colNums);
		if (! t.hasIndex(tblnum, colNumsStr)) // if not bootstrap
			t.addIndex(tblnum, colNumsStr);
		insertExistingData(index);
		// TODO handle foreign keys
	}

	private void insertExistingData(Index newIndex) {
		Table table = t.getTable(tableName);
		if (table == null)
			return;
		Btree src = t.getIndex(tblnum, table.firstIndex().colNumsString());
		Btree btree = t.addIndex(tblnum, newIndex.colNumsString());
		Btree.Iter iter = src.iterator();
		IndexedData id = new IndexedData(t.tran);
		id.index(btree, newIndex.mode(), newIndex.colNums);
		for (iter.next(); ! iter.eof(); iter.next())
			id.add(t.getrec(Btree.getAddress(iter.cur())));
	}

	public void dropIndex(String columnNames) {
		int[] colNums = colNums(columnNames);
		for (int i = 0; i < indexes.size(); ++i) {
			Index index = indexes.get(i);
			if (Arrays.equals(colNums, index.colNums)) {
				t.removeRecord(TN.INDEXES, index.toRecord());
				indexes.remove(i);
				return;
			}
		}
		fail(CANT_DROP + NONEXISTENT_INDEX + " (" + columnNames + ")");
	}

	private static final CharMatcher cm = CharMatcher.is(',');
	private static final Splitter splitter = Splitter.on(',');

	private int[] colNums(String s) {
		int[] cols = new int[cm.countIn(s) + 1];
		int i = 0;
		for (String c : splitter.split(s))
			cols[i++] = colNum(c);
		return cols;
	}

	private int colNum(String column) {
		int i = findColumn(column);
		if (i == -1)
			fail(NONEXISTENT_COLUMN + ": " + column);
		return columns.get(i).field;
	}

	private boolean hasColumn(String column) {
		return findColumn(column) != -1;
	}

	private boolean hasIndex(String columnNames) {
		int[] colNums = colNums(columnNames);
		for (Index index : indexes)
			if (Arrays.equals(colNums, index.colNums))
				return true;
		return false;
	}

	// build -------------------------------------------------------------------

	public void build() {
		mustHaveKey();
		updateSchema();
		updateTableInfo();
	}

	private void mustHaveKey() {
		for (Index index : indexes)
			if (index.isKey)
				return;
		fail(TABLE_MUST_HAVE_KEY);
	}

	private void updateSchema() {
		Collections.sort(indexes); // to match SchemaLoader
		t.addSchemaTable(new Table(tblnum, tableName,
				new Columns(ImmutableList.copyOf(columns)),
				new Indexes(ImmutableList.copyOf(indexes))));
	}

	private void updateTableInfo() {
		ImmutableList.Builder<IndexInfo> ii = ImmutableList.builder();
		for (Index index : indexes)
			ii.add(new IndexInfo(index.colNumsString(),
					t.getIndex(tblnum, index.colNums).info()));
		TableInfo ti = t.dbinfo.get(tblnum);
		int nrows = (ti == null) ? 0 : ti.nrows();
		long totalsize = (ti == null) ? 0 : ti.totalsize();
		t.addTableInfo(new TableInfo(tblnum,
				columns.size(), nrows, totalsize, ii.build()));
	}

	private void fail(String msg) {
		fail(t, msg);
	}
	private static void fail(UpdateTransaction t, String msg) {
		t.abort();
		throw new RuntimeException(msg);
	}

	public void finish() {
		build();
		t.commit();
	}

	public void abortUnfinished() {
		t.abortIfNotCommitted();
	}

}
