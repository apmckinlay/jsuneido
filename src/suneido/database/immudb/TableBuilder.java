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
		int tblnum = t.getTable(tableName).num;
		TableBuilder tb = new TableBuilder(t, tableName, tblnum);
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
		int field = columns.size();
		Column c = new Column(tblnum, field, column);
		t.addRecord(TN.COLUMNS, c.toRecord());
		columns.add(c);
	}

	public void removeColumn(String column) {
		// TODO removeColumn
	}

	public void ensureIndex(String columnNames, boolean isKey, boolean unique,
			String fktable, String fkcolumns, int fkmode) {
		if (! hasIndex(columnNames))
			addIndex(columnNames, isKey, unique, fktable, fkcolumns, fkmode);
	}

	public void addIndex(String columnNames, boolean isKey, boolean unique,
			String fktable, String fkcolumns, int fkmode) {
		int[] colnums = nums(columnNames);
		Index index = new Index(tblnum, colnums, isKey, unique);
		t.addRecord(TN.INDEXES, index.toRecord());
		indexes.add(index);
		String colnumsStr = Ints.join(",", colnums);
		if (! t.hasIndex(tblnum, colnumsStr)) // if not bootstrap
			t.addIndex(tblnum, colnumsStr);
		insertExistingData(index);
		// TODO handle foreign keys
	}

	private void insertExistingData(Index newIndex) {
		Table table = t.getTable(tableName);
		if (table == null)
			return;
		Btree src = t.getIndex(tblnum, table.firstIndex().columnsString());
		Btree btree = t.addIndex(tblnum, newIndex.columnsString());
		Btree.Iter iter = src.iterator();
		IndexedData id = new IndexedData(t.tran);
		id.index(btree, newIndex.mode(), newIndex.columns);
		for (iter.next(); ! iter.eof(); iter.next())
			id.add(t.getrec(Btree.getAddress(iter.cur())));
	}

	public void removeIndex(String columnNames) {
		// TODO removeIndex
	}

	private static final CharMatcher cm = CharMatcher.is(',');
	private static final Splitter splitter = Splitter.on(',');

	private int[] nums(String s) {
		int[] cols = new int[cm.countIn(s) + 1];
		int i = 0;
		for (String c : splitter.split(s))
			cols[i++] = field(c);
		return cols;
	}

	private int field(String field) {
		for (Column c : columns)
			if (c.name.equals(field))
				return c.field;
		throw new RuntimeException();
	}

	private boolean hasColumn(String column) {
		for (Column c : columns)
			if (c.name.equals(column))
				return true;
		return false;
	}

	private boolean hasIndex(String columnNames) {
		int[] columns = nums(columnNames);
		for (Index index : indexes)
			if (Arrays.equals(columns, index.columns))
				return true;
		return false;
	}

	public void build() {
		Collections.sort(indexes);
		t.addSchemaTable(new Table(tblnum, tableName,
				new Columns(ImmutableList.copyOf(columns)),
				new Indexes(ImmutableList.copyOf(indexes))));
		ImmutableList.Builder<IndexInfo> ii = ImmutableList.builder();
		for (Index index : indexes)
			ii.add(new IndexInfo(index.columnsString(),
					t.getIndex(tblnum, index.columns).info()));
		TableInfo ti = t.dbinfo.get(tblnum);
		int nrows = (ti == null) ? 0 : ti.nrows();
		long totalsize = (ti == null) ? 0 : ti.totalsize();
		t.addTableInfo(new TableInfo(tblnum,
				columns.size(), nrows, totalsize, ii.build()));
	}

	public void finish() {
		build();
		t.commit();
	}

	public void abortUnfinished() {
		t.abortUncommitted();
	}

}
