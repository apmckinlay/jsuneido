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
	private final String name;
	private final int tblnum;
	private final Transaction t;
	private final List<Column> columns = Lists.newArrayList();
	private final List<Index> indexes = Lists.newArrayList();
	private final List<Btree> btrees = Lists.newArrayList();
	private final ImmutableList.Builder<Column> cb = ImmutableList.builder();
	private final ImmutableList.Builder<Index> ib = ImmutableList.builder();

	public static TableBuilder builder(Transaction t, String name) {
		TableBuilder tb = new TableBuilder(t, name);
		tb.createTable(name);
		return tb;
	}

	private TableBuilder(Transaction t, String name) {
		this.t = t;
		this.name = name;
		this.tblnum = nextTableNum();
	}

	private int nextTableNum() {
		// TODO
		return 4;
	}

	private void createTable(String name) {
		Btree tables = t.getIndex("tables");
		IndexedData id = new IndexedData().index(tables, 0);
		int tblnum = 4; // TODO next table num
		Record r = Table.toRecord(tblnum, name);
		id.add(t.tran, r);
	}

	public void addColumn(String column) {
		Btree btree = t.getIndex("columns");
		IndexedData id = new IndexedData().index(btree, 0, 2);
		int field = columns.size();
		Column c = new Column(tblnum, field, column);
		Record r = c.toRecord();
		id.add(t.tran, r);
		columns.add(c);
		cb.add(new Column(tblnum, field, column));
	}

	public void addIndex(String columns, boolean key, boolean unique,
			String fktable, String fkcolumns, int fkmode) {
		Btree indexesIndex = t.getIndex("indexes");
		IndexedData id = new IndexedData().index(indexesIndex, 0, 1);
		int[] colnums = nums(columns);
		Index index = new Index(tblnum, colnums, key, unique);
		Record r = index.toRecord();
		id.add(t.tran, r);
		indexes.add(index);
		Btree btree = new Btree(t.tran);
		btrees.add(btree);
		t.indexes.put(name, btree);
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
		t.schema = t.schema.with(new Table(tblnum, name,
				new Columns(cb.build()), new Indexes(ib.build())));
		t.dbinfo.add(new TableInfo(tblnum, columns.size(), 0, 0, ImmutableList.of(
				new IndexInfo(indexes.get(0).columnsString(), btrees.get(0).info()))));
		// TODO handle multiple indexes
		t.commit();
	}

}
