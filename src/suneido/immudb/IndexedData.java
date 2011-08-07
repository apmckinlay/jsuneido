/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordinates data records and the btrees that index them.
 */
class IndexedData {
	enum Mode { KEY, UNIQUE, DUPS };
	private final Tran tran;
	private final List<AnIndex> indexes = new ArrayList<AnIndex>();

	IndexedData(Tran tran) {
		this.tran = tran;
	}

	/** setup method */
	IndexedData index(Btree btree, Mode mode, int... fields) {
		indexes.add(new AnIndex(btree, mode, fields));
		return this;
	}

	// TODO foreign keys

	int add(Record rec) {
		int intref = tran.refToInt(rec);
		for (AnIndex index : indexes)
			if (! index.add(rec, intref)) {
				// undo previous add's
				for (AnIndex idx : indexes) {
					if (idx == index)
						break;
					idx.remove(rec, intref);
				}
				throw new RuntimeException("duplicate key: " +
						index.searchKey(rec)); // TODO better msg
			}
		return intref;
	}

	void remove(Record rec) {
		int intref = firstKey().getKeyAdr(rec);
		if (intref == 0)
			throw new RuntimeException("remove couldn't find record");
		for (AnIndex index : indexes)
			if (! index.remove(rec, intref))
				throw new RuntimeException("remove failed");
		// TODO handle remove failing halfway through (abort transaction?)
	}

	void update(Record from, Record to) {
		int fromIntref = firstKey().getKeyAdr(from);
		if (fromIntref == 0)
			throw new RuntimeException("update couldn't find record");
		int toIntref = tran.refToInt(to);
		for (AnIndex index : indexes)
			if (! index.update(from, fromIntref, to, toIntref))
				throw new RuntimeException("update failed");
		// TODO handle remove failing halfway through (abort transaction?)
	}

	private AnIndex firstKey() {
		for (AnIndex index : indexes)
			if (index.mode == Mode.KEY)
				return index;
		throw new RuntimeException("no key!");
	}

	static class AnIndex {
		final Btree btree;
		final Mode mode;
		final int[] fields;

		AnIndex(Btree btree, Mode mode, int[] fields) {
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

	static Record key(Record rec, int[] fields, int adr) {
		return new RecordBuilder().addFields(rec, fields).adduint(adr).build();
	}

	static boolean isEmptyKey(Record key) {
		for (int i = 0; i < key.size() - 1; ++i)
			if (key.fieldLength(i) != 0)
				return false;
		return true;
	}

}
