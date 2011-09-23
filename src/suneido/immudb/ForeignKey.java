/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

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
		int hash = 17;
		hash = 31 * hash + tablename.hashCode();
		hash = 31 * hash + columns.hashCode();
		return hash;
	}

}