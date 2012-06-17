/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.util.Arrays;

import javax.annotation.concurrent.Immutable;

import suneido.intfc.database.Fkmode;

import com.google.common.base.Objects;

@Immutable
class ForeignKeyTarget {
	final String tablename;
	final int tblnum;
	final int[] colNums;
	final int mode;

	ForeignKeyTarget(int tblnum, String tablename, int[] colNums, int mode) {
		assert ! tablename.equals("");
		this.mode = mode;
		this.colNums = colNums;
		this.tablename = tablename;
		this.tblnum = tblnum;
	}

	// need equals and hashCode to store in Set

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (! (other instanceof ForeignKeyTarget))
			return false;
		ForeignKeyTarget that = (ForeignKeyTarget) other;
		return this.tblnum == that.tblnum &&
				Arrays.equals(this.colNums, that.colNums) &&
				this.mode == that.mode;
	}

	@Override
	public int hashCode() {
		int hash = 17;
		hash = 31 * hash + tblnum;
		hash = 31 * hash + Arrays.hashCode(colNums);
		hash = 31 * hash + mode;
		return hash;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(tablename)
				.addValue(Arrays.toString(colNums))
				.addValue(Fkmode.toString(mode))
				.toString();
	}

}