/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import javax.annotation.concurrent.NotThreadSafe;

import suneido.database.immudb.schema.*;
import suneido.database.immudb.schema.Table;
import suneido.database.immudb.schema.Tables;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.*;

@NotThreadSafe
public class Transaction {
	private final Storage stor;
	private final Tran tran;
	private final DbInfo dbinfo;
	private final Tables schema;
	private final Map<String,Btree> indexes = Maps.newHashMap();
	private TableBuilder tb = null;

	public Transaction(Storage stor, int dbinfo, int redirs, Tables schema) {
		this.stor = stor;
		tran = new Tran(stor, redirs);
		this.dbinfo = new DbInfo(tran, dbinfo);
		this.schema = schema;
	}

	public Btree getIndex(String table) { // TODO handle multiple indexes
		Btree btree = indexes.get(table);
		if (btree != null)
			return btree;
		Table tbl = schema.get(table);
		TableInfo ti = dbinfo.get(tbl.num);
		btree = new Btree(tran, ti.firstIndex());
		indexes.put(table, btree);
		return btree;
	}

	public class TableBuilder {
		final int tblnum;
		final List<Column> columns = Lists.newArrayList();
		final List<Index> indexes = Lists.newArrayList();
 		final List<Btree> btrees = Lists.newArrayList();

		TableBuilder(int tblnum) {
			this.tblnum = tblnum;
		}

		int field(String field) {
			for (Column c : columns)
				if (c.name.equals(field))
					return c.field;
			throw new RuntimeException();
		}

		public void build() {
			// TODO handle multiple indexes
			dbinfo.add(new TableInfo(tblnum, columns.size(), 0, 0, ImmutableList.of(
					new IndexInfo(indexes.get(0).columnsString(), btrees.get(0).info()))));
		}
	} // end of TableBuilder

	public TableBuilder createTable(String name) {
		Btree btree = getIndex("tables");
		IndexedData id = new IndexedData().index(btree, 0);
		int tblnum = 4; // TODO next table num
		Record r = Table.toRecord(tblnum, name);
		id.add(tran, r);
		tb = new TableBuilder(tblnum);
		return tb;
	}

	public void addColumn(TableBuilder tb, String column) {
		Btree btree = getIndex("columns");
		IndexedData id = new IndexedData().index(btree, 0, 2);
		int field = tb.columns.size();
		Column c = new Column(tb.tblnum, field, column);
		Record r = c.toRecord();
		id.add(tran, r);
		tb.columns.add(c);
	}

	private static final CharMatcher cm = CharMatcher.is(',');
	private static final Splitter splitter = Splitter.on(',');

	public int[] nums(TableBuilder tb, String s) {
		int[] cols = new int[cm.countIn(s) + 1];
		int i = 0;
		for (String c : splitter.split(s))
			cols[i++] = tb.field(c);
		return cols;
	}

	public void addIndex(TableBuilder tb, String columns, boolean key,
			boolean unique, String fktable, String fkcolumns, int fkmode) {
		Btree btree = getIndex("indexes");
		IndexedData id = new IndexedData().index(btree, 0, 1);
		Index index = new Index(tb.tblnum, nums(tb, columns), key, unique);
		Record r = index.toRecord();
		id.add(tran, r);
		tb.indexes.add(index);
		tb.btrees.add(new Btree(tran));
	}

	// TODO synchronize
	public void commit() {
		tran.startStore();
		DataRecords.store(tran);
		Btree.store(tran);

		if (tb != null)
			tb.build();
		updateDbInfo();
		// TODO update nrows, totalsize

		int redirs = tran.storeRedirs();
		store(dbinfo.store(), redirs);
		tran.endStore();
	}

	private void updateDbInfo() {
		for (Map.Entry<String, Btree> e : indexes.entrySet()) {
			String tableName = e.getKey();
			int tblnum = schema.get(tableName).num;
			Btree btree = e.getValue();
			TableInfo ti = dbinfo.get(tblnum);
			ti = new TableInfo(tblnum, ti.nextfield, ti.nrows, ti.totalsize,
					ImmutableList.of(
							new IndexInfo(ti.firstIndex().columns, btree.info())));
			dbinfo.add(ti);
		}
	}

	static final int INT_SIZE = 4;

	// TODO remove duplication with Bootstrap
	private void store(int dbinfo, int redirs) {
		ByteBuffer buf = stor.buffer(stor.alloc(2 * INT_SIZE));
		buf.putInt(dbinfo);
		buf.putInt(redirs);
	}

}
