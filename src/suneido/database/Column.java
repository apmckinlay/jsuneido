package suneido.database;

public class Column implements Comparable<Column> {
	public String name;
	public short num;

	@SuppressWarnings("unused")
	private final static int TBLNUM = 0, COLUMN = 1, FLDNUM = 2;

	public Column(Record r) {
		name = r.getString(COLUMN);
		num = r.getShort(FLDNUM);
	}

	public Column(String column, short colnum) {
		this.name = column;
		this.num = colnum;
	}

	public int compareTo(Column other) {
		return num - other.num;
	}

	public static Record record(int table_num, String name, int num) {
		Record r = new Record();
		r.add(table_num);
		r.add(name);
		r.add(num);
		return r;
	}
}
