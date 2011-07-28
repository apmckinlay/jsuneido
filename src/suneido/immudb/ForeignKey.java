/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import javax.annotation.concurrent.Immutable;

@Immutable
class ForeignKey {
	final String tablename; // used by fksrc
	final int tblnum; // used by fkdsts
	final String columns;
	final int mode;

	ForeignKey(String tablename, String columns, int mode) {
		this(tablename, columns, mode, 0);
	}

	ForeignKey(int tblnum, String columns, int mode) {
		this(null, columns, mode, tblnum);
	}

	private ForeignKey(String tablename, String columns, int mode, int tblnum) {
		this.mode = mode;
		this.columns = columns;
		this.tablename = tablename;
		this.tblnum = tblnum;
	}

	@Override
	public String toString() {
		return "ForeignKey(" + (tablename == null ? tblnum : tablename) +
				", " + columns + ", " + mode + ")";
	}
}