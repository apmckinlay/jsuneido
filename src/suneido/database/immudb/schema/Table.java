/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb.schema;

import java.util.List;

import javax.annotation.concurrent.Immutable;

import suneido.database.immudb.Record;
import suneido.database.immudb.RecordBuilder;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

/**
 * Table schema information.
 */
@Immutable
public class Table {
	public final static int TBLNUM = 0, TABLE = 1, NEXTFIELD = 2, NROWS = 3, TOTALSIZE = 4;
	public final int num;
	public final String name;
	public final int nextfield;
	public final int nrows;
	public final long totalsize;
	public final Columns columns;
	public final Indexes indexes;
	public final ImmutableList<String> fields;

	public Table(int num, String name, int nextfield, int nrows, long totalsize,
			Columns columns, Indexes indexes) {
		this.num = num;
		this.name = name;
		this.nextfield = nextfield;
		this.nrows = nrows;
		this.totalsize = totalsize;
		this.columns = columns;
		this.indexes = indexes;
		fields = buildFields();
	}

	public Table(Record record, Columns columns, Indexes indexes) {
		num = record.getInt(TBLNUM);
		name = record.getString(TABLE);
		nextfield = record.getInt(NEXTFIELD);
		nrows = record.getInt(NROWS);
		totalsize = record.getLong(TOTALSIZE);
		this.columns = columns;
		this.indexes = indexes;
		fields = buildFields();
	}

	public Record toRecord() {
		return toRecord(num, name, nextfield, nrows, totalsize);
	}

	public static Record toRecord(int num, String name,
			int nextfield, int nrows, long totalsize) {
		return new RecordBuilder()
			.add(num).add(name).add(nextfield).add(nrows).add(totalsize)
			.build();
	}

	public boolean hasColumn(String name) {
		return columns.hasColumn(name);
	}

	public Column getColumn(String name) {
		return columns.find(name);
	}

	public int maxColumnNum() {
		return columns.maxNum();
	}

	public boolean hasIndexes() {
		return !indexes.isEmpty();
	}

	public boolean hasIndex(String columns) {
		return indexes.hasIndex(columns);
	}

	public Index firstIndex() {
		return indexes.first();
	}

	public Index getIndex(String columns) {
		return indexes.get(columns);
	}

	public boolean singleton() {
		return indexes.first().columns.equals("");
	}
	public List<String> getColumns() {
		return columns.names();
	}
	public List<List<String>> indexesColumns() {
		return indexes.columns();
	}
	public List<List<String>> keysColumns() {
		return indexes.keysColumns();
	}

	/**
	 * @return The physical fields. 1:1 match with records.
	 */
	public ImmutableList<String> getFields() {
		return fields;
	}

	private ImmutableList<String> buildFields() {
		ImmutableList.Builder<String> list = ImmutableList.builder();
		int i = 0;
		for (Column cs : columns) {
			if (cs.colnum < 0)
				continue; // skip rules
			for (; i < cs.colnum; ++i)
				list.add("-");
			list.add(cs.name);
			++i;
		}
		return list.build();
	}

	public String schema() {
		StringBuilder sb = new StringBuilder();

		// fields
		sb.append("(").append(columns.schemaColumns()).append(")");

		// indexes
		for (Index index : indexes) {
			if (index.isKey)
				sb.append(" key");
			else
				sb.append(" index").append(index.unique ? " unique" : "");
			sb.append("(").append(index.columns).append(")");
			if (index.fksrc != null && !index.fksrc.tablename.equals("")) {
				sb.append(" in ").append(index.fksrc.tablename);
				if (!index.fksrc.columns.equals(index.columns))
					sb.append("(").append(index.fksrc.columns).append(")");
				if (index.fksrc.mode == Index.CASCADE)
					sb.append(" cascade");
				else if (index.fksrc.mode == Index.CASCADE_UPDATES)
					sb.append(" cascade update");
			}
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("name", name)
				.add("num", num)
				.add("columns", columns)
				.add("indexes", indexes)
				.toString();
	}

}
