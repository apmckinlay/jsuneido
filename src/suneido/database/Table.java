package suneido.database;

public class Table {
	public String name;
	public int num;
	public int nextfield;
	public int nrecords;
	public int totalsize;
	public final Columns columns = new Columns();
	public final Indexes indexes = new Indexes();
	final static int TBLNUM = 0, TABLE = 1, NEXTFIELD = 2, NROWS = 3,
			TOTALSIZE = 4;

	public Table(Record r) {
		num = r.getInt(TBLNUM);
		name = r.getString(TABLE);
		nextfield = r.getInt(NEXTFIELD);
		nrecords = r.getInt(NROWS);
		totalsize = r.getInt(TOTALSIZE);
	}
	public void addColumn(Column column) {
		columns.add(column);
	}
	public void sortColumns() {
		columns.sort();
	}
	public void addIndex(Idx idx) {
		indexes.add(idx);
	}

	public boolean hasColumn(String name) {
		return columns.hasColumn(name);
	}

	@Override
	public String toString() {
		return "Table('" + name + "', " + num + ")";
	}

	public void user_trigger(int tran, Record norec, Record r) {
		// TODO Auto-generated method stub
	}

	public void update() {
		// TODO Auto-generated method stub
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
	public Idx firstIndex() {
		return indexes.first();
	}

}
