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
	public enum Mode { KEY, UNIQUE, DUPS };
	private final Tran tran;
	private final List<Index> indexes = new ArrayList<Index>();

	public IndexedData(Tran tran) {
		this.tran = tran;
	}

	/** setup method */
	public IndexedData index(Btree btree, Mode mode, int... fields) {
		indexes.add(new Index(btree, mode, fields));
		return this;
	}

	public int add(Record data) {
		int intref = tran.refToInt(data);
		for (Index index : indexes)
			if (! index.add(data, intref)) {
				// undo previous add's
				for (Index idx : indexes) {
					if (idx == index)
						break;
					idx.remove(data, intref);
				}
				throw new RuntimeException("duplicate key"); // TODO better msg
			}
		return intref;
	}

	private static class Index {
		final Btree btree;
		final Mode mode;
		final int[] fields;

		public Index(Btree btree, Mode mode, int[] fields) {
			this.btree = btree;
			this.mode = mode;
			this.fields = fields;
		}

		public boolean add(Record data, int intref) {
			Record key = key(data, fields, intref);
			boolean unique = (mode == Mode.KEY ||
					(mode == Mode.UNIQUE && ! isEmptyKey(key)));
			return btree.add(key, unique);
		}

		public void remove(Record data, int intref) {
			Record key = key(data, fields, intref);
			btree.remove(key);
		}
	}

	public static Record key(Record rec, int[] fields, int adr) {
		RecordBuilder rb = new RecordBuilder();
		for (int field : fields)
			rb.add(rec, field);
		rb.add(adr);
		return rb.build();
	}

	public static boolean isEmptyKey(Record key) {
		for (int i = 0; i < key.size() - 1; ++i)
			if (key.fieldLength(i) != 0)
				return false;
		return true;
	}

}
