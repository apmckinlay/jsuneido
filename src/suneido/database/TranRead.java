package suneido.database;

/**
 * Stores the range of keys that was read by an {@link BtreeIndex} iterator.
 * Used to validate transactions.
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small></p>
 */
public class TranRead {
	int tblnum;
	String index;
	Record org = Record.MINREC;
	Record end = Record.MAXREC;
	
	TranRead(int tblnum, String index) {
		this.tblnum = tblnum;
		this.index = index;
	}
}
