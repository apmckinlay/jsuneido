package suneido.database;

import static suneido.Suneido.verify;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small></p>
 */
public class Table {
	private final Record record;
	public final String name;
	public final int num;
	public final Columns columns = new Columns();
	public final Indexes indexes = new Indexes();
	public int nextfield;
	public int nrecords;
	public int totalsize;
	final static int TBLNUM = 0, TABLE = 1, NEXTFIELD = 2, NROWS = 3,
			TOTALSIZE = 4;

	public Table(Record record) {
		this.record = record;
		num = record.getInt(TBLNUM);
		name = record.getString(TABLE);
		nextfield = record.getInt(NEXTFIELD);
		nrecords = record.getInt(NROWS);
		totalsize = record.getInt(TOTALSIZE);
	}
	@Override
	public String toString() {
		return "Table('" + name + "', " + num + ")";
	}

	public void addColumn(Column column) {
		columns.add(column);
	}

	public void sortColumns() {
		columns.sort();
	}

	public void addIndex(Index index) {
		indexes.add(index);
	}

	public boolean hasColumn(String name) {
		return columns.hasColumn(name);
	}

	public Column getColumn(String name) {
		return columns.find(name);
	}

	public void user_trigger(Transaction tran, Record norec, Record r) {
		// TODO Auto-generated method stub
	}

	public void update() {
		verify(record.off() != 0);
		record.truncate(NEXTFIELD);
		record.add(nextfield).add(nrecords).add(totalsize);
	}

	public static Record record(String name, int num, int nextfield, int nrecords) {
		Record r = new Record();
		r.add(num).add(name).add(nextfield).add(nrecords).add(100);
		r.alloc(24); // 24 = 3 fields * max int packsize - min int packsize
		return r;
	}

	public boolean hasIndexes() {
		return !indexes.isEmpty();
	}

	public boolean hasRecords() {
		return nrecords > 0;
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
	public List<String> get_columns() {
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
	public List<String> get_fields() {
		List<String> list = new ArrayList<String>();
		int i = 0;
		for (Column cs : columns) {
			if (cs.num < 0)
				continue; // skip rules
			for (; i < cs.num; ++i)
				list.add("-");
			list.add(cs.name);
			++i;
		}
		for (; i < nextfield; ++i)
			list.add("-");
		return list;
	}

	public String schema() {
		StringBuilder sb = new StringBuilder();

		// fields
		sb.append("(");
		for (String col : get_columns())
			if (!col.equals("-"))
				sb.append(col).append(",");
		// for (String f : get_rules(table))
		// {
		// gcstring str(f->str()); // copy
		// char* s = str.str();
		// *s = toupper(*s);
		// sb.append(s).append(",");
		// }
		sb.deleteCharAt(sb.length() - 1);
		sb.append(")");

		// indexes
		for (Index index : indexes) {
			if (index.isKey())
				sb.append(" key");
			else
				sb.append(" index").append(
						index.btreeIndex.unique ? " unique" : "");
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
}
