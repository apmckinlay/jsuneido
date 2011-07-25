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
class Table implements suneido.intfc.database.Table {
	public static final int TBLNUM = 0, TABLE = 1;
	public final int num;
	public final String name;
	public final Columns columns;
	public final Indexes indexes;
	public final ImmutableList<String> fields;

	Table(int num, String name, Columns columns, Indexes indexes) {
		this.num = num;
		this.name = name;
		this.columns = columns;
		this.indexes = indexes;
		fields = buildFields();
	}

	Table(Record record, Columns columns, Indexes indexes) {
		num = record.getInt(TBLNUM);
		name = record.getString(TABLE);
		this.columns = columns;
		this.indexes = indexes;
		fields = buildFields();
	}

	@Override
	public int num() {
		return num;
	}

	Record toRecord() {
		return toRecord(num, name);
	}

	static Record toRecord(int num, String name) {
		return new RecordBuilder().add(num).add(name).build();
	}

	boolean hasColumn(String name) {
		return columns.hasColumn(name);
	}

	Column getColumn(String name) {
		return columns.find(name);
	}

	int maxColumnNum() {
		return columns.maxNum();
	}

	boolean hasIndexes() {
		return !indexes.isEmpty();
	}

//	boolean hasIndex(String columns) {
//		return indexes.hasIndex(columns);
//	}

	Index firstIndex() {
		return indexes.first();
	}

//	Index getIndex(String columns) {
//		return indexes.get(columns);
//	}

	@Override
	public boolean singleton() {
		return indexes.first().colNums.length == 0;
	}

	@Override
	public List<String> getColumns() {
		return columns.names();
	}

	@Override
	public List<List<String>> indexesColumns() {
		return indexes.columns(fields);
	}

	@Override
	public List<List<String>> keysColumns() {
		return indexes.keysColumns(fields);
	}

	List<Column> columnsList() {
		return columns.columns;
	}

	List<Index> indexesList() {
		return indexes.indexes;
	}

	/**
	 * @return The physical fields. 1:1 match with records.
	 */
	@Override
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

	@Override
	public String schema() {
		StringBuilder sb = new StringBuilder();
		sb.append("(").append(columns.schemaColumns()).append(")");
		for (Index index : indexes)
			index.schema(sb, columns);
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
