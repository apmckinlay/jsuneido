package suneido.database;

import java.util.List;

import javax.annotation.concurrent.Immutable;

import suneido.database.Database.TN;

import com.google.common.collect.ImmutableList;

/**
 * Table schema information.
 * The mutable data: nextfield, nrows, and totalsize are stored in {@link TableData}
 * @author Andrew McKinlay
 */
@Immutable
public class Table {
	final static int TBLNUM = 0, TABLE = 1, NEXTFIELD = 2, NROWS = 3, TOTALSIZE = 4;
	public final String name;
	public final int num;
	public final Columns columns;
	public final Indexes indexes;
	public final ImmutableList<String> fields;

	public Table(Record record, Columns columns, Indexes indexes) {
		this.columns = columns;
		this.indexes = indexes;
		this.fields = get_fields();
		num = record.getInt(TBLNUM);
		name = record.getString(TABLE);
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

	private ImmutableList<String> get_fields() {
		ImmutableList.Builder<String> list = ImmutableList.builder();
		int i = 0;
		for (Column cs : columns) {
			if (cs.num < 0)
				continue; // skip rules
			for (; i < cs.num; ++i)
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

	public static Record record(String name, int num,
			int nextfield, int nrecords) {
		return record(name, num, nextfield, nrecords, totalsize(num));
	}

	public static Record record(String name, int num,
			int nextfield, int nrecords, int totalsize) {
		Record r = new Record();
		r.add(num).add(name).add(nextfield).add(nrecords).add(totalsize);
		r.alloc(24); // 24 = 3 fields * max int packsize - min int packsize
		return r;
	}

	private static int totalsize(int num) {
		switch (num) {
		case TN.TABLES:
			return 176;
		case TN.COLUMNS:
			return 390;
		case TN.INDEXES:
			return 331;
		default:
			return 0;
		}
	}

	public static void update(Record record, int nextfield, int nrecords, int totalsize) {
		assert record.off() != 0;
		int n = record.packSize();
		record.truncate(Table.NEXTFIELD);
		record.add(nextfield).add(nrecords).add(totalsize);
		record.alloc(n - record.packSize() - (n < 256 ? 1 : 2));
		assert n == record.packSize();
		assert nextfield == record.getInt(Table.NEXTFIELD);
		assert nrecords == record.getInt(Table.NROWS);
		assert totalsize == record.getInt(Table.TOTALSIZE);
	}

	@Override
	public String toString() {
		return "Table(" + name + ":" + num + ") " + columns + " " + indexes;
	}

}
