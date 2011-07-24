/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.util.List;

import javax.annotation.concurrent.Immutable;


import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

/**
 * Table schema information.
 */
@Immutable
public class Table {
	public final static int TBLNUM = 0, TABLE = 1;
	public final int num;
	public final String name;
	public final Columns columns;
	public final Indexes indexes;
	public final ImmutableList<String> fields;

	public Table(int num, String name, Columns columns, Indexes indexes) {
		this.num = num;
		this.name = name;
		this.columns = columns;
		this.indexes = indexes;
		fields = buildFields();
	}

	public Table(Record record, Columns columns, Indexes indexes) {
		num = record.getInt(TBLNUM);
		name = record.getString(TABLE);
		this.columns = columns;
		this.indexes = indexes;
		fields = buildFields();
	}

	public Record toRecord() {
		return toRecord(num, name);
	}

	public static Record toRecord(int num, String name) {
		return new RecordBuilder().add(num).add(name).build();
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

//	public boolean hasIndex(String columns) {
//		return indexes.hasIndex(columns);
//	}

	public Index firstIndex() {
		return indexes.first();
	}

//	public Index getIndex(String columns) {
//		return indexes.get(columns);
//	}

	public boolean singleton() {
		return indexes.first().colNums.length == 0;
	}
	public List<String> getColumns() {
		return columns.names();
	}
//	public List<List<String>> indexesColumns() {
//		return indexes.columns();
//	}
//	public List<List<String>> keysColumns() {
//		return indexes.keysColumns();
//	}

	public List<Column> columnsList() {
		return columns.columns;
	}

	public List<Index> indexesList() {
		return indexes.indexes;
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
			if (cs.field < 0)
				continue; // skip rules
			for (; i < cs.field; ++i)
				list.add("-");
			list.add(cs.name);
			++i;
		}
		return list.build();
	}

	public String schema() {
		StringBuilder sb = new StringBuilder();
		sb.append("(").append(columns.schemaColumns()).append(")");
		for (Index index : indexes)
			index.schema(sb, columns);
//		{
//			if (index.isKey)
//				sb.append(" key");
//			else
//				sb.append(" index").append(index.unique ? " unique" : "");
//			sb.append("(").append(index.columns).append(")");
//			if (index.fksrc != null && !index.fksrc.tablename.equals("")) {
//				sb.append(" in ").append(index.fksrc.tablename);
//				if (!index.fksrc.columns.equals(index.columns))
//					sb.append("(").append(index.fksrc.columns).append(")");
//				if (index.fksrc.mode == Index.CASCADE)
//					sb.append(" cascade");
//				else if (index.fksrc.mode == Index.CASCADE_UPDATES)
//					sb.append(" cascade update");
//			}
//		}
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
