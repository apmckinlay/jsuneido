package suneido.database;

import static suneido.Suneido.verify;

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

	@Override
	public String toString() {
		return "Table('" + name + "', " + num + ")";
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
}
