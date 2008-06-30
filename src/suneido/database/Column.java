package suneido.database;

/**
 * 
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small></p>
 */
public class Column implements Comparable<Column> {
	public final String name;
	public final short num;

	@SuppressWarnings("unused")
	private final static int TBLNUM = 0, COLUMN = 1, FLDNUM = 2;

	public Column(Record record) {
		name = record.getString(COLUMN);
		num = record.getShort(FLDNUM);
	}

	public Column(String column, short colnum) {
		this.name = column;
		this.num = colnum;
	}

	public int compareTo(Column other) {
		return num - other.num;
	}

	public static Record record(int table_num, String name, int num) {
		return new Record().add(table_num).add(name).add(num);
	}
}
