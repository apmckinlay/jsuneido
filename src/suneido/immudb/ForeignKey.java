/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Objects;

/*
 * Base class for {@link ForeignKeySource} and {@link ForeignKeyTarget}
 * <p>
 * Have to keep tablename and columns as strings (rather than tblnum and colnums)
 * because the table may not exist yet.
 */
@Immutable
class ForeignKey {
	protected final String tablename;
	protected final String columns;

	public ForeignKey(String tablename, String columns) {
		assert ! tablename.equals("");
		this.tablename = tablename;
		this.columns = columns;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (! (other instanceof ForeignKey))
			return false;
		ForeignKey that = (ForeignKey) other;
		return tablename.equals(that.tablename) &&
				columns.equals(that.columns);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(tablename, columns);
	}

}