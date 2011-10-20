/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.util.Verify.verify;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import suneido.immudb.Bootstrap.TN;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;

class TableBuilder implements suneido.intfc.database.TableBuilder {
	static final String COLUMN_IN_INDEX = "column used in index";
	static final String KEY_REQUIRED = "key required";
	static final String NONEXISTENT_TABLE = "nonexistent table";
	static final String NONEXISTENT_COLUMN = "nonexistent column";
	static final String NONEXISTENT_INDEX = "nonexistent index";
	static final String CANT_DROP = "can't drop ";
	static final String CANT_RENAME = "can't rename ";
	static final String TO_EXISTING = "to existing column";
	private ReadTransaction t;
	private final String tableName;
	private final int tblnum;
	private final List<Column> columns = Lists.newArrayList();
	private final List<Index> indexes = Lists.newArrayList();
	private int nextField = 0;

	static TableBuilder create(ExclusiveTransaction t, String tableName, int tblNum) {
		if (t.getTable(tableName) != null)
			fail(t, "can't create existing table: " + tableName);
		TableBuilder tb = new TableBuilder(t, tableName, tblNum);
		tb.createTable();
		return tb;
	}

	static TableBuilder alter(ReadTransaction rt, String tableName) {
		return new TableBuilder(rt, tableName);
	}

	private TableBuilder(ReadTransaction et, String tableName, int tblnum) {
		this.t = et;
		this.tableName = tableName;
		this.tblnum = tblnum;
	}

	private TableBuilder(ReadTransaction t, String tableName) {
		this(t, tableName, tblnum(t, tableName));
		nextField = t.getTableInfo(tblnum).nextfield;
		getSchema();
	}

	private static int tblnum(ReadTransaction t, String tableName) {
		Table table = t.getTable(tableName);
		if (table == null)
			fail(t, NONEXISTENT_TABLE + ": " + tableName);
		return table.num;
	}

	private void createTable() {
		et().addRecord(TN.TABLES, Table.toRecord(tblnum, tableName));
	}

	static boolean dropTable(ExclusiveTransaction t, String tableName) {
		if (null != Views.getView(t, tableName))
			Views.dropView(t, tableName);
		else {
			Table table = t.getTable(tableName);
			if (table == null) {
				t.abort();
				return false;
			}
			TableBuilder tb = alter(t, tableName);
			for (Index index : table.indexes)
				verify(tb.dropIndex(index.colNums));
			for (Column column : table.columns)
				tb.dropColumn(column.name);
			t.removeRecord(TN.TABLES, table.toRecord());
			t.dropTableSchema(table);
			// NOTE: leaves dbinfo (DbHashTrie doesn't have remove)
		}
		t.complete();
		return true;
	}

	static void renameTable(ExclusiveTransaction t, String from, String to) {
		Table oldTable = t.getTable(from);
		if (oldTable == null)
			fail(t, CANT_RENAME + NONEXISTENT_TABLE + ": " + from);
		if (t.getTable(to) != null)
			fail(t, CANT_RENAME + " to existing table name");
		t.dropTableSchema(oldTable);
		Table newTable = new Table(oldTable.num, to,
				new Columns(ImmutableList.copyOf(oldTable.columns)),
				new Indexes(ImmutableList.copyOf(oldTable.indexes)));
		t.addSchemaTable(newTable);
		// dbinfo is ok since it doesn't use table name
		t.updateRecord(TN.TABLES, oldTable.toRecord(), newTable.toRecord());
		t.complete();
	}

	private void getSchema() {
		Table table = t.getTable(tableName);
		columns.addAll(table.columnsList());
		indexes.addAll(table.indexesList());
	}

	@Override
	public TableBuilder ensureColumn(String column) {
		if (! hasColumn(column))
			addColumn2(column);
		return this;
	}

	@Override
	public TableBuilder addColumn(String column) {
		if (hasColumn(column))
			throw new RuntimeException("add column: column already exists: "
					+ column + " in " + tableName);
		addColumn2(column);
		return this;
	}

	public void addColumn2(String column) {
		int field = isRuleField(column) ? -1 : nextField++;
		if (field == -1)
			column = column.substring(0, 1).toLowerCase() + column.substring(1);
		Column c = new Column(tblnum, field, column);
		et().addRecord(TN.COLUMNS, c.toRecord());
		columns.add(c);
	}

	private static boolean isRuleField(String column) {
		return Character.isUpperCase(column.charAt(0));
	}

	@Override
	public TableBuilder renameColumn(String from, String to) {
		if (hasColumn(to))
			fail(CANT_RENAME + TO_EXISTING + ": " + to);
		int i = findColumn(columns, from);
		if (i == -1)
			fail(CANT_RENAME + NONEXISTENT_COLUMN + ": " + from);
		Column c = columns.get(i);
		Record oldRec = c.toRecord();
		c = new Column(tblnum, c.field, to);
		columns.set(i, c);
		Record newRec = c.toRecord();
		t.updateRecord(TN.COLUMNS, oldRec, newRec);
		// don't need to update indexes because they use column numbers not names
		return this;
	}

	@Override
	public TableBuilder dropColumn(String column) {
		int i = findColumn(columns, column);
		if (i == -1)
			fail(CANT_DROP + NONEXISTENT_COLUMN + ": " + column);
		Column c = columns.get(i);
		mustNotBeUsedByIndex(column);
		t.removeRecord(TN.COLUMNS, c.toRecord());
		columns.remove(i);
		return this;
	}

	private void mustNotBeUsedByIndex(String column) {
		int colNum = colNum(columns, column);
		for (int i = 0; i < indexes.size(); ++i) {
			Index index = indexes.get(i);
			if (Ints.contains(index.colNums, colNum))
				fail(CANT_DROP + COLUMN_IN_INDEX + ": " + column);
		}
	}

	@Override
	public TableBuilder ensureIndex(String columnNames, boolean isKey, boolean unique,
			String fktable, String fkcolumns, int fkmode) {
		if (! hasIndex(columnNames))
			addIndex2(columnNames, isKey, unique, fktable, fkcolumns, fkmode);
		return this;
	}

	@Override
	public TableBuilder addIndex(String columnNames, boolean isKey, boolean unique,
			String fktable, String fkcolumns, int fkmode) {
		if (hasIndex(columnNames))
			throw new RuntimeException("add index: index already exists: " +
					columnNames + " in " + tableName);
		addIndex2(columnNames, isKey, unique, fktable, fkcolumns, fkmode);
		return this;
	}

	public void addIndex2(String columnNames, boolean isKey, boolean unique,
			String fktable, String fkcolumns, int fkmode) {
		int[] colNums = colNums(columnNames);
		Index index = new Index(tblnum, colNums, isKey, unique,
				fktable, fkcolumns, fkmode);
		et().addRecord(TN.INDEXES, index.toRecord());
		indexes.add(index);
		if (! t.hasIndex(tblnum, index.colNums)) // if not bootstrap
			et().addIndex(tblnum, index.colNums);
	}

	@Override
	public TableBuilder dropIndex(String columnNames) {
		int[] colNums = colNums(columnNames);
		if (! dropIndex(colNums))
			fail(CANT_DROP + NONEXISTENT_INDEX + " (" + columnNames + ")");
		return this;
	}

	private boolean dropIndex(int[] colNums) {
		for (int i = 0; i < indexes.size(); ++i) {
			Index index = indexes.get(i);
			if (Arrays.equals(colNums, index.colNums)) {
				t.removeRecord(TN.INDEXES, index.toRecord());
				indexes.remove(i);
				return true;
			}
		}
		return false;
	}

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
		for (String c : splitter.split(s))
			cols[i++] = colNum(columns, c);
		return cols;
	}

	private int colNum(List<Column> columns, String column) {
		int i = findColumn(columns, column);
		if (i == -1)
			fail(NONEXISTENT_COLUMN + ": " + column);
		return columns.get(i).field;
	}

	private static int findColumn(List<Column> columns, String column) {
		for (int i = 0; i < columns.size(); ++i) {
			Column c = columns.get(i);
			if (c.name.equals(column))
				return i;
		}
		return -1;
	}

	private boolean hasColumn(String column) {
		return findColumn(columns, column) != -1;
	}

	private boolean hasIndex(String columnNames) {
		int[] colNums = colNums(columnNames);
		for (Index index : indexes)
			if (Arrays.equals(colNums, index.colNums))
				return true;
		return false;
	}

	// build -------------------------------------------------------------------

	/** for Bootstrap, normally should call finish() instead */
	void build() {
		mustHaveKey();
		updateSchema();
		for (Index index : indexes)
			insertExistingData(index);
		updateTableInfo();
	}

	private void mustHaveKey() {
		for (Index index : indexes)
			if (index.isKey)
				return;
		fail(KEY_REQUIRED + " for: " + tableName);
	}

	private void updateSchema() {
		Collections.sort(columns);
		Collections.sort(indexes); // to match SchemaLoader
		et().updateSchemaTable(new Table(tblnum, tableName,
				new Columns(ImmutableList.copyOf(columns)),
				new Indexes(ImmutableList.copyOf(indexes))));
	}

	private void insertExistingData(Index newIndex) {
		Table table = t.getTable(tableName);
		if (table == null)
			return;
		Btree src = t.getIndex(tblnum, table.firstIndex().colNums);
		Btree.Iter iter = src.iterator();
		Btree btree = et().addIndex(tblnum, newIndex.colNums);
		IndexedData id = new IndexedData(et())
				.index(btree, newIndex.mode(), newIndex.colNums, newIndex.fksrc,
						t.getForeignKeys(tableName,
								table.numsToNames(newIndex.colNums)));
		for (iter.next(); ! iter.eof(); iter.next()) {
			int adr = iter.keyadr();
			id.add(t.getrec(adr), adr);
		}
	}

	private void updateTableInfo() {
		ImmutableList.Builder<IndexInfo> ii = ImmutableList.builder();
		for (Index index : indexes)
			ii.add(new IndexInfo(index.colNums,
					t.getIndex(tblnum, index.colNums).info()));
		TableInfo ti = t.getTableInfo(tblnum);
		int nrows = (ti == null) ? 0 : ti.nrows();
		long totalsize = (ti == null) ? 0 : ti.totalsize();
		et().addTableInfo(
				new TableInfo(tblnum, nextField, nrows, totalsize, ii.build()));
	}

	private void fail(String msg) {
		fail(t, msg);
	}
	private static void fail(ReadTransaction t, String msg) {
		t.abort();
		throw new RuntimeException(msg);
	}

	@Override
	public void finish() {
		if (t instanceof ExclusiveTransaction)
			build();
		t.complete();
	}

	@Override
	public void abortUnfinished() {
		t.abortIfNotComplete();
	}

	private ExclusiveTransaction et() {
		if (t instanceof ExclusiveTransaction)
			return (ExclusiveTransaction) t;
		else {
			ExclusiveTransaction et = t.db.exclusiveTran();
			t.complete();
			t = et;
			return et;
		}
	}

}
