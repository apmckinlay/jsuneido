/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import suneido.util.IntArrayList;

public class DataRecords {
	private final IntArrayList records = new IntArrayList();

	public void add(int intref) {
		records.add(intref);
	}

	/**
	 * Write out all the data Record's.
	 * Must ensure database addresses are ordered the same as intrefs
	 * so btree ordering does not change.
	 */
	public void store(Tran tran) {
		for (int i = 0; i < records.size(); ++i) {
			int intref = records.get(i);
			Record r = (Record) tran.intToRef(intref);
			tran.setAdr(intref, r.storeRecord(tran.stor));
		}
	}

}
