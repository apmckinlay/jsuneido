/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

public class DataRecords {

	/**
	 * Write out all the data Record's.
	 * Must ensure database addresses are ordered the same as intrefs
	 * so btree ordering does not change.
	 */
	public static void store(Tran tran) {
		int i = -1;
		for (Object x : tran.context.intrefs) {
			++i;
			if (! (x instanceof Record))
				continue;
			int intref = i | IntRefs.MASK;
			Record r = (Record) tran.intToRef(intref);
			tran.setAdr(intref, r.store(tran.context.stor));
		}
	}

}
