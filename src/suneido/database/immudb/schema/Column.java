/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb.schema;

import javax.annotation.concurrent.Immutable;

import suneido.database.immudb.Record;
import suneido.database.immudb.RecordBuilder;

@Immutable
public class Column implements Comparable<Column> {
	public static final int TBLNUM = 0, COLUMN = 1, FLDNUM = 2;
	public final int tblnum;
	public final String name;
	public final int colnum;

	public Column(int tblnum, String column, int colnum) {
		this.tblnum = tblnum;
		this.name = column;
		this.colnum = colnum;
	}

	public Column(Record record) {
		tblnum = record.getInt(TBLNUM);
		name = record.getString(COLUMN);
		colnum = record.getInt(FLDNUM);
	}

	public Record toRecord() {
		return toRecord(tblnum, name, colnum);
	}

	public static Record toRecord(int tblnum, String name, int colnum) {
		return new RecordBuilder().add(tblnum).add(name).add(colnum).build();
	}

	public int compareTo(Column other) {
		return colnum - other.colnum;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof Column))
			return false;
		return colnum == ((Column) other).colnum;
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return name + ":" + colnum;
	}

}
