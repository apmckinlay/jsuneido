/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordinates data records and the btrees that index them.
 */
public class IndexedData {
	List<Index> indexes = new ArrayList<Index>();

	/** setup method */
	public IndexedData index(Btree btree, int... fields) {
		indexes.add(new Index(btree, fields));
		return this;
	}

	public void add(Tran tran, Record data) {
		int intref = tran.refRecordToInt(data);
		for (Index index : indexes)
			index.add(data, intref);
	}

	private static class Index {
		final Btree btree;
		final int[] fields;

		public Index(Btree btree, int[] fields) {
			this.btree = btree;
			this.fields = fields;
		}

		public void add(Record data, int intref) {
			btree.add(key(data, intref));
		}

		private Record key(Record data, int intref) {
			RecordBuilder rb = new RecordBuilder();
			for (int field : fields)
				rb.add(data, field);
			rb.add(intref);
			return rb.build();
		}
	}

}
