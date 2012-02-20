/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

class DataRecords {

	/**
	 * Write out all the data Record's.
	 * Must ensure database addresses are ordered the same as intrefs
	 * so btree ordering does not change.
	 */
	static void store(Tran tran) {
		int i = -1;
		for (Object x : tran.intrefs) {
			++i;
			if (! (x instanceof Record))
				continue;
			int intref = i | IntRefs.MASK;
			assert (Record) tran.intToRef(intref) == x;
			tran.setAdr(intref, ((Record) x).store(tran.stor));
		}
	}

}
