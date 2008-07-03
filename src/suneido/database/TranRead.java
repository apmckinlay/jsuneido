package suneido.database;

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
	public String toString() {
		return "TranRead(table: " + tblnum + ", index: " + index + ", " + org
		+ " ... " + end + ")";
	}
}
