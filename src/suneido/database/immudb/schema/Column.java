/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb.schema;

import javax.annotation.concurrent.Immutable;

import suneido.database.immudb.Record;
import suneido.database.immudb.RecordBuilder;

@Immutable
public class Column implements Comparable<Column> {
	public static final int TBLNUM = 0, FLDNUM = 1, COLUMN = 2;
	public final int tblnum;
	public final int field;
	public final String name;

	public Column(int tblnum, int field, String column) {
		this.tblnum = tblnum;
		this.name = column;
		this.field = field;
	}

	public Column(Record record) {
		tblnum = record.getInt(TBLNUM);
		field = record.getInt(FLDNUM);
		name = record.getString(COLUMN);
	}

	public Record toRecord() {
		return toRecord(tblnum, field, name);
	}

	public static Record toRecord(int tblnum, int field, String column) {
		return new RecordBuilder().add(tblnum).add(field).add(column).build();
	}

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
