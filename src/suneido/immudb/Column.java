/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import javax.annotation.concurrent.Immutable;

import suneido.immudb.Bootstrap.TN;

@Immutable
class Column implements Comparable<Column> {
	static final int TBLNUM = 0, FLDNUM = 1, COLUMN = 2;
	final int tblnum;
	final int field;
	final String name;

	Column(int tblnum, int field, String column) {
		this.tblnum = tblnum;
		this.name = column;
		this.field = field;
	}

	Column(Record record) {
		tblnum = record.getInt(TBLNUM);
		field = record.getInt(FLDNUM);
		name = record.getString(COLUMN);
	}

	Record toRecord() {
		return toRecord(tblnum, field, name);
	}

	static Record toRecord(int tblnum, int field, String column) {
		Record r = new RecordBuilder().add(tblnum).add(field).add(column).build();
		r.tblnum = TN.COLUMNS;
		return r;
	}

	@Override
	public int compareTo(Column other) {
		return field - other.field;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof Column))
			return false;
		return field == ((Column) other).field;
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return name + ":" + field;
	}

}
