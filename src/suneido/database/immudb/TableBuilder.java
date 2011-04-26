/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.util.List;

import suneido.database.immudb.schema.*;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class TableBuilder {
	private final String tblname;
	private final int tblnum;
	private final Transaction t;
	private final List<Column> columns = Lists.newArrayList();
	private final List<Index> indexes = Lists.newArrayList();
	private final ImmutableList.Builder<Column> cb = ImmutableList.builder();
	private final ImmutableList.Builder<Index> ib = ImmutableList.builder();

	public static TableBuilder builder(Transaction t, String name, int tblnum) {
		TableBuilder tb = new TableBuilder(t, name, tblnum);
		tb.createTable();
		return tb;
	}

	private TableBuilder(Transaction t, String tblname, int tblnum) {
		this.t = t;
		this.tblname = tblname;
		this.tblnum = tblnum;
	}

	private void createTable() {
		Record r = Table.toRecord(tblnum, tblname);
		t.addRecord(r, "tables", 0);
	}

	public void addColumn(String column) {
		int field = columns.size();
		Column c = new Column(tblnum, field, column);
		Record r = c.toRecord();
		t.addRecord(r, "columns", 0, 1);
		columns.add(c);
		cb.add(new Column(tblnum, field, column));
	}

	public void addIndex(String columns, boolean key, boolean unique,
			String fktable, String fkcolumns, int fkmode) {
		int[] colnums = nums(columns);
		Index index = new Index(tblnum, colnums, key, unique);
		Record r = index.toRecord();
		t.addRecord(r, "indexes", 0, 1);
		indexes.add(index);
		if (! t.hasIndex(tblname)) // if not bootstrap
			t.addIndex(tblname);
		ib.add(new Index(tblnum, colnums, key, unique));
		// TODO handle foreign keys
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

	public void build() {
		t.addSchemaTable(new Table(tblnum, tblname,
				new Columns(cb.build()), new Indexes(ib.build())));
		t.addTableInfo(new TableInfo(tblnum, columns.size(), 0, 0, ImmutableList.of(
				new IndexInfo(indexes.get(0).columnsString(), t.getIndex(tblname).info()))));
		// TODO handle multiple indexes
	}

	public void finish() {
		build();
		t.commit();
	}

}
