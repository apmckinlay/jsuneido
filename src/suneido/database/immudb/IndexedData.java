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

	public int add(Record rec) {
		int intref = tran.refToInt(rec);
		for (Index index : indexes)
			if (! index.add(rec, intref)) {
				// undo previous add's
				for (Index idx : indexes) {
					if (idx == index)
						break;
					idx.remove(rec, intref);
				}
				throw new RuntimeException("duplicate key: " +
						index.searchKey(rec)); // TODO better msg
			}
		return intref;
	}

	public void remove(Record rec) {
		int intref = firstKey().getKeyAdr(rec);
		if (intref == 0)
			throw new RuntimeException("remove couldn't find record");
		for (Index index : indexes)
			if (! index.remove(rec, intref))
				throw new RuntimeException("remove failed");
		// TODO handle remove failing halfway through (abort transaction?)
	}

	public void update(Record from, Record to) {
		int fromIntref = firstKey().getKeyAdr(from);
		if (fromIntref == 0)
			throw new RuntimeException("update couldn't find record");
		int toIntref = tran.refToInt(to);
		for (Index index : indexes)
			if (! index.update(from, fromIntref, to, toIntref))
				throw new RuntimeException("update failed");
		// TODO handle remove failing halfway through (abort transaction?)
	}

	private Index firstKey() {
		for (Index index : indexes)
			if (index.mode == Mode.KEY)
				return index;
		throw new RuntimeException("no key!");
	}

	private static class Index {
		final Btree btree;
		final Mode mode;
		final int[] fields;

		Index(Btree btree, Mode mode, int[] fields) {
			this.btree = btree;
			this.mode = mode;
			this.fields = fields;
		}

		boolean update(Record from, int fromIntref, Record to, int toIntref) {
			Record fromKey = key(from, fields, fromIntref);
			Record toKey = key(to, fields, toIntref);
			boolean unique = (mode == Mode.KEY ||
					(mode == Mode.UNIQUE && ! isEmptyKey(toKey)));
			return btree.update(fromKey, toKey, unique);
		}

		boolean add(Record rec, int intref) {
			Record key = key(rec, fields, intref);
			boolean unique = (mode == Mode.KEY ||
					(mode == Mode.UNIQUE && ! isEmptyKey(key)));
			return btree.add(key, unique);
		}

		boolean remove(Record rec, int intref) {
			Record key = key(rec, fields, intref);
			return btree.remove(key);
		}
		int getKeyAdr(Record rec) {
			return btree.get(searchKey(rec));
		}
		Record searchKey(Record rec) {
			return new RecordBuilder().addFields(rec, fields).build();
		}
	}

	public static Record key(Record rec, int[] fields, int adr) {
		return new RecordBuilder().addFields(rec, fields).add(adr).build();
	}

	public static boolean isEmptyKey(Record key) {
		for (int i = 0; i < key.size() - 1; ++i)
			if (key.fieldLength(i) != 0)
				return false;
		return true;
	}

}
