/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import javax.annotation.concurrent.Immutable;

import suneido.intfc.database.Fkmode;

import com.google.common.base.MoreObjects;

/**
 * Used by {@link Index}
 * @see ForeignKeyTarget
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
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.addValue(tablename)
				.addValue(columns)
				.addValue(Fkmode.toString(mode))
				.toString();
	}

}
