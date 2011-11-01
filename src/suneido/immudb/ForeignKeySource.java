/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import javax.annotation.concurrent.Immutable;

import suneido.intfc.database.Fkmode;

/*
 * Have to keep tablename and columns as strings (rather than tblnum and colnums)
 * because the table may not exist yet.
 */
@Immutable
class ForeignKeySource extends ForeignKey {
	final int mode;

	ForeignKeySource(String tablename, String columns, int mode) {
		super(tablename, columns);
		assert ! columns.equals("");
		this.mode = mode;
	}

	@Override
	public boolean equals(Object other) {
		return super.equals(other);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public String toString() {
		return "ForeignKeySource(" + tablename + ", " +
				columns + ", " + Fkmode.toString(mode) + ")";
	}

}
