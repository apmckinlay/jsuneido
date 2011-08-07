/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import javax.annotation.concurrent.Immutable;

@Immutable
class ForeignKey {
	final String tablename; // used by fksrc
	final int tblnum; // used by fkdsts
	final int[] colNums;
	final int mode;

	ForeignKey(String tablename, int[] colNums, int mode) {
		this(tablename, colNums, mode, 0);
	}

	ForeignKey(int tblnum, int[] colNums, int mode) {
		this(null, colNums, mode, tblnum);
	}

	private ForeignKey(String tablename, int[] colNums, int mode, int tblnum) {
		this.mode = mode;
		this.colNums = colNums;
		this.tablename = tablename;
		this.tblnum = tblnum;
	}

	@Override
	public String toString() {
		return "ForeignKey(" + (tablename == null ? tblnum : tablename) +
				", " + colNums + ", " + mode + ")";
	}
}