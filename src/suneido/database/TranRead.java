package suneido.database;

import com.google.common.base.Objects;

/**
 * Stores the range of keys that was read by an {@link BtreeIndex} iterator.
 * Used to validate transactions.
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small></p>
 */
public class TranRead implements Comparable<TranRead> {
	int tblnum;
	String index;
	// empty range:
	Record org = Record.MAXREC;
	Record end = Record.MINREC;

	TranRead(int tblnum, String index) {
		this.tblnum = tblnum;
		this.index = index;
	}

	public int compareTo(TranRead other) {
		return tblnum != other.tblnum ? tblnum - other.tblnum :
			index.compareTo(other.index);
	}
	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof TranRead))
			return false;
		TranRead tr = (TranRead) other;
		return tblnum == tr.tblnum &&
				Objects.equal(index, tr.index);
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("tblnum", tblnum)
				.add("index", index)
				.add("org", org)
				.add("end", end)
				.toString();
	}
}
