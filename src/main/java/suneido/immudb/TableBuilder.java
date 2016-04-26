/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.util.Util.uncapitalize;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;

import suneido.SuException;
import suneido.immudb.Bootstrap.TN;
import suneido.util.Util;

class TableBuilder implements suneido.intfc.database.TableBuilder {
	private final SchemaTransaction t;
	private final String tableName;
	private final int tblnum;
	private final List<Column> columns = Lists.newArrayList();
	private final List<Index> indexes = Lists.newArrayList();
	private final List<Index> newIndexes = Lists.newArrayList();
	private Index firstIndex;
	private int nextField = 0;

	static TableBuilder create(SchemaTransaction t, String tableName, int tblnum) {
		return new TableBuilder(t, tableName, tblnum).createTable();
	}

	static TableBuilder alter(SchemaTransaction t, String tableName) {
		return new TableBuilder(t, tableName);
	}

	static TableBuilder ensure(SchemaTransaction t, String tableName, int tblnum) {
		return (t.getTable(tableName) == null)
				? create(t, tableName, tblnum)
				: alter(t, tableName);
	}

	private TableBuilder(SchemaTransaction t, String tableName, int tblnum) {
		this.t = t;
		this.tableName = tableName;
		this.tblnum = tblnum;
	}

	private TableBuilder(SchemaTransaction t, String tableName) {
		this(t, tableName, tblnum(t, tableName));
		nextField = t.getTableInfo(tblnum).nextfield;
		getSchema();
	}

	int tblnum() {
		return tblnum;
	}

	private static int tblnum(SchemaTransaction t, String tableName) {
		Table table = t.getTable(tableName);
		if (table == null)
			throw fail(t, "alter table: nonexistent table: " + tableName);
		return table.num;
	}

	private TableBuilder createTable() {
		verify(t.getTable(tableName) == null,
				"create table: table already exists: " + tableName);
		t.addRecord(TN.TABLES, Table.toRecord(tblnum, tableName));
		return this;
	}

	static boolean dropTable(SchemaTransaction t, String tableName) {
		// check for view first
		if (! Views.dropView(t, tableName)) {
			Table table = t.getTable(tableName);
			if (table == null) {
				t.abort();
				return false;
			}
			t.removeRecord(TN.TABLES, table.toRecord());
			for (Index index : table.indexes)
				t.removeRecord(TN.INDEXES, index.toRecord());
			for (Column column : table.columns)
				t.removeRecord(TN.COLUMNS, column.toRecord());
			t.dropTable(table);
		}
		t.ck_complete();
		return true;
	}

	static void renameTable(SchemaTransaction t, String from, String to) {
		Table oldTable = t.getTable(from);
		if (oldTable == null)
			throw fail(t, "rename table: nonexistent table: " + from);
		if (t.getTable(to) != null)
			fail(t, "rename table: table already exists: " + to);
		Table newTable = new Table(oldTable.num, to,
				oldTable.columns, oldTable.indexes);
		t.updateTableSchema(newTable);
		// dbinfo is ok since it doesn't use table name
		t.updateRecord(TN.TABLES, oldTable.toRecord(), newTable.toRecord());
		t.ck_complete();
	}

	private void getSchema() {
		Table table = t.getTable(tableName);
		columns.addAll(table.columnsList());
		indexes.addAll(table.indexesList());
		firstIndex = indexes.get(0);
	}

	@Override
	public TableBuilder ensureColumn(String column) {
		if (! hasColumn(column))
			addColumn(column);
		return this;
	}

	@Override
	public TableBuilder addColumn(String column) {
		verify(! hasColumn(column),
				"add column: column already exists: " + column);
		boolean isRuleField = isRuleField(column);
		if (isRuleField)
			column = uncapitalize(column);
		int field = isRuleField ? -1
				: isSpecialField(column) ? baseField(column)
				: nextField++;
		Column c = new Column(tblnum, field, column);
		columns.add(c);
		t.addRecord(TN.COLUMNS, c.toRecord());
		return this;
	}

	private static boolean isRuleField(String column) {
		return Character.isUpperCase(column.charAt(0));
	}

	//TODO: eliminate duplication with Request
	private static boolean isSpecialField(String column) {
		return column.endsWith("_lower!");
	}

	private int baseField(String column) {
		String base = Util.beforeLast(column, "_");
		int fld = colNum(base);
		return -fld - 2; // offset by 2 because 0 and -1 are taken
	}

	@Override
	public TableBuilder renameColumn(String from, String to) {
		verify(hasColumn(from),
				"rename column: nonexistent column: " + from);
		verify(! hasColumn(to),
				"rename column: column already exists: " + to);
		int i = findColumn(from);
		Column cOld = columns.get(i);
		Column cNew = new Column(tblnum, cOld.field, to);
		t.updateRecord(TN.COLUMNS, cOld.toRecord(), cNew.toRecord());
		columns.set(i, cNew);
		return this;
	}

	@Override
	public TableBuilder dropColumn(String column) {
		verify(hasColumn(column),
				"drop column: nonexistent column: " + column);
		mustNotBeUsedByIndex(column);
		int i = findColumn(column);
		t.removeRecord(TN.COLUMNS, columns.get(i).toRecord());
		columns.remove(i);
		return this;
	}

	private void mustNotBeUsedByIndex(String column) {
		int colNum = colNum(column);
		for (int i = 0; i < indexes.size(); ++i) {
			Index index = indexes.get(i);
			verify(! Ints.contains(index.colNums, colNum),
				"drop column: column used in index: " + column);
		}
	}

	@Override
	public TableBuilder ensureIndex(String colNames, boolean isKey, boolean unique,
			String fktable, String fkcolumns, int fkmode) {
		int[] colNums = colNums(colNames);
		if (! hasIndex(colNums))
			addIndex(colNums, isKey, unique, fktable, fkcolumns, fkmode);
		return this;
	}

	@Override
	public TableBuilder addIndex(String colNames, boolean isKey, boolean unique,
			String fktable, String fkcolumns, int fkmode) {
		int[] colNums = colNums(colNames);
		verify(! hasIndex(colNums),
				"add index: index already exists: " + colNames);
		addIndex(colNums, isKey, unique, fktable, fkcolumns, fkmode);
		return this;
	}

	void addIndex(int[] colNums, boolean isKey, boolean unique,
			String fktable, String fkcolumns, int fkmode) {
		Index index = new Index(tblnum, colNums, isKey, unique,
				fktable, fkcolumns, fkmode);
		addIndex(index);
	}

	private void addIndex(Index index) {
		t.addRecord(TN.INDEXES, index.toRecord());
		t.addIndex(index);
		indexes.add(index);
		newIndexes.add(index);
	}

	@Override
	public TableBuilder dropIndex(String colNames) {
		int[] colNums = colNums(colNames);
		verify(hasIndex(colNums),
				"drop index: nonexistent index: " + colNames);
		dropIndex(colNums);
		return this;
	}

	void dropIndex(int[] colNums) {
		Index index = indexes.get(findIndex(colNums));
		t.removeRecord(TN.INDEXES, index.toRecord());
		indexes.remove(findIndex(colNums));
	}

	//--------------------------------------------------------------------------

	private static final int[] noColumns = new int[0];
	private static final CharMatcher cm = CharMatcher.is(',');
	private static final Splitter splitter = Splitter.on(',');

	private int[] colNums(String s) {
		if (s == null)
			return null;
		if (s.equals(""))
			return noColumns;
		int[] cols = new int[cm.countIn(s) + 1];
		int i = 0;
		for (String c : splitter.split(s)) {
			int cn = colNum(c);
			verify(cn != -1, "cannot index rule field " + c);
			cols[i++] = cn;
		}
		return cols;
	}

	/** @return the field number of the column, throws if not found */
	private int colNum(String column) {
		int i = findColumn(column);
		if (i == -1)
			fail("nonexistent column: " + column);
		return columns.get(i).field;
	}

	/** @return the index of the column or -1 if not found */
	private int findColumn(String column) {
		if (isRuleField(column))
			column = uncapitalize(column);
		for (int i = 0; i < columns.size(); ++i)
			if (columns.get(i).name.equals(column))
				return i;
		return -1;
	}

	private boolean hasColumn(String column) {
		return findColumn(column) >= 0;
	}

	private boolean hasIndex(int[] colNums) {
		return findIndex(colNums) >= 0;
	}

	int findIndex(int[] colNums) {
		for (int i = 0; i < indexes.size(); ++i)
			if (Arrays.equals(colNums, indexes.get(i).colNums))
				return i;
		return -1;
	}

	//--------------------------------------------------------------------------

	@Override
	public void finish() {
		try {
			buildButDontComplete();
			t.ck_complete();
		} finally {
			t.abortIfNotComplete();
		}
	}

	/** used by Bootstrap */
	void buildButDontComplete() {
		mustHaveKey();
		updateSchema();
		for (Index index : newIndexes)
			insertExistingData(index);
		updateTableInfo();
	}

	private void mustHaveKey() {
		for (Index index : indexes)
			if (index.isKey)
				return;
		fail("key required");
	}

	private void updateSchema() {
		Collections.sort(columns);
		Collections.sort(indexes); // to match SchemaLoader
		t.updateTableSchema(new Table(tblnum, tableName,
				new Columns(ImmutableList.copyOf(columns)),
				new Indexes(ImmutableList.copyOf(indexes))));
	}

	private void insertExistingData(Index newIndex) {
		Table table = t.getTable(tableName);
		if (table == null)
			return;
		if (firstIndex == null)
			return;
		Btree btree = (Btree) t.getIndex(newIndex);
		TranIndex src = t.getIndex(tblnum, firstIndex.colNums);
		if (src instanceof OverlayIndex)
			src = ((OverlayIndex) src).global;
		TranIndex.Iter iter = src.iterator();
		iter.next();
		if (iter.eof())
			return; // no data
		t.exclusive();
		String colNames = table.numsToNames(newIndex.colNums);
		IndexedData id = new IndexedData(t)
				.index(btree, newIndex.mode(), newIndex.colNums, colNames,
						newIndex.fksrc, t.getForeignKeys(tableName, colNames));
		for (; ! iter.eof(); iter.next()) {
			int adr = iter.keyadr();
			id.add(t.input(adr), adr);
		}
		btree.freeze();
	}

	private void updateTableInfo() {
		TableInfo ti = t.getTableInfo(tblnum);
		ImmutableList.Builder<IndexInfo> ii = ImmutableList.builder();
		if (ti != null && ti.indexInfo != null)
			ii.addAll(ti.indexInfo);
		for (Index index : indexes) {
			TranIndex idx = t.getIndex(tblnum, index.colNums);
			if (idx instanceof Btree)
				ii.add(new IndexInfo(index.colNums, idx.info()));
		}
		int nrows = (ti == null) ? 0 : ti.nrows();
		long totalsize = (ti == null) ? 0 : ti.totalsize();
		t.addTableInfo(
				new TableInfo(tblnum, nextField, nrows, totalsize, ii.build()));
	}

	private void verify(boolean cond, String msg) {
		if (! cond)
			fail(msg);
	}
	private void fail(String msg) {
		fail(t, msg + " in " + tableName);
	}
	private static SuException fail(ReadTransaction t, String msg) {
		t.abort();
		throw new SuException(msg);
	}

	@Override
	public void abortUnfinished() {
		t.abortIfNotComplete();
	}

}
