/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

import javax.annotation.concurrent.Immutable;

@Immutable
public class Column implements Comparable<Column> {
	public final String name;
	public final int num;

	public static final int TBLNUM = 0;
	public static final int COLUMN = 1;
	public static final int FLDNUM = 2;

	public Column(Record record) {
		name = record.getString(COLUMN);
		num = record.getShort(FLDNUM);
	}

	public Column(String column, int colnum) {
		this.name = column;
		this.num = colnum;
	}

	public int compareTo(Column other) {
		return num - other.num;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof Column))
			return false;
		return num == ((Column) other).num;
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
	}

	public static Record record(int table_num, String name, int num) {
		return new Record().add(table_num).add(name).add(num);
	}

	@Override
	public String toString() {
		return name + ":" + num;
	}

}
