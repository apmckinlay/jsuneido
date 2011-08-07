/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.util.Util.commaSplitter;

import java.util.List;

import javax.annotation.concurrent.Immutable;

import suneido.immudb.Bootstrap.TN;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * Table schema information.
 */
@Immutable
class Table implements suneido.intfc.database.Table {
	static final int TBLNUM = 0, TABLE = 1;
	final int num;
	final String name;
	final Columns columns;
	final Indexes indexes;
	final ImmutableList<String> fields;

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
	public String name() {
		return name;
	}

	@Override
	public int num() {
		return num;
	}

	Record toRecord() {
		return toRecord(num, name);
	}

	static Record toRecord(int num, String name) {
		Record r = new RecordBuilder().add(num).add(name).build();
		r.tblnum = TN.TABLES;
		return r;
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

	int[] namesToNums(String names) {
		Iterable<String> cs = commaSplitter.split(names);
		int[] nums = new int[Iterables.size(cs)];
		int c = 0;
		for (String field : cs)
			nums[c++] = fields.indexOf(field);
		return nums;
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

	public String schema(ReadTransaction t) {
		StringBuilder sb = new StringBuilder();
		sb.append("(").append(columns.schemaColumns()).append(")");
		for (Index index : indexes)
			index.schema(sb, columns, t);
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
