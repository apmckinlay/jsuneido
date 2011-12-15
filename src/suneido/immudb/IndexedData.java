/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import suneido.intfc.database.Fkmode;

/**
 * Coordinates data records and the btrees that index them.
 */
class IndexedData {
	enum Mode { KEY, UNIQUE, DUPS };
	private final UpdateTransaction t;
	private final Tran tran;
	private final List<AnIndex> indexes = new ArrayList<AnIndex>();

	IndexedData(UpdateTransaction t) {
		this.t = t;
		tran = t.tran;
	}

	// for tests (without foreign keys)
	IndexedData(Tran tran) {
		this.tran = tran;
		t = null;
	}

	/** setup method */
	IndexedData index(Btree btree, Mode mode, int[] colNums, String colNames,
			ForeignKeySource fksrc, Set<ForeignKeyTarget> fkdsts) {
		assert t != null;
		indexes.add(new AnIndexWithFkeys(
				t, btree, mode, colNums, colNames, fksrc, fkdsts));
		return this;
	}

	// for tests (without foreign keys)
	IndexedData index(Btree btree, Mode mode, int... fields) {
		indexes.add(new AnIndex(btree, mode, fields, ""));
		return this;
	}

	int add(Record rec) {
		int intref = tran.refToInt(rec);
		add(rec, intref);
		return intref;
	}

	// used by TableBuilder to add indexes to existing tables
	void add(Record rec, int adr) {
		for (AnIndex index : indexes)
			index.fkeyHandleAdd(rec);
		for (AnIndex index : indexes)
			if (! index.add(rec, adr)) {
				// undo previous add's
				for (AnIndex idx : indexes) {
					if (idx == index)
						break;
					idx.remove(rec, adr);
				}
				throw new RuntimeException("duplicate key: " +
						index.columns + " = " + index.searchKey(rec));
			}
	}

	void remove(Record rec) {
		for (AnIndex index : indexes)
			index.fkeyHandleRemove(rec);
		int adr = rec.address();
		if (adr == 0)
			adr = firstKey().getKeyAdr(rec);
		if (adr == 0)
			throw new RuntimeException("remove couldn't find record");
		for (AnIndex index : indexes)
			if (! index.remove(rec, adr))
				throw new RuntimeException("remove failed");
		// TODO handle remove failing halfway through (abort transaction?)
	}

	void update(Record from, Record to) {
		for (AnIndex index : indexes)
			index.fkeyHandleUpdate(from, to);

		int fromIntref = firstKey().getKeyAdr(from);
		if (fromIntref == 0)
			throw new RuntimeException("update couldn't find record");
		int toIntref = tran.refToInt(to);
		for (AnIndex index : indexes)
			switch (index.update(from, fromIntref, to, toIntref)) {
			case NOT_FOUND:
				throw new RuntimeException("update failed: old record not found");
			case ADD_FAILED:
				throw new RuntimeException("update failed: duplicate key" +
						index.columns + " = " + index.searchKey(to));
			case OK:
				break;
			default:
				throw new RuntimeException("unhandled update result");
			}
		// TODO handle update failing halfway through (abort transaction?)
	}

	private AnIndex firstKey() {
		for (AnIndex index : indexes)
			if (index.mode == Mode.KEY)
				return index;
		throw new RuntimeException("no key!");
	}

	private static class AnIndex {
		final Btree btree;
		final Mode mode;
		final int[] fields;
		final String columns;

		AnIndex(Btree btree, Mode mode, int[] fields, String columns) {
			this.btree = btree;
			this.mode = mode;
			this.fields = fields;
			this.columns = columns;
		}

		Btree.Update update(Record from, int fromAdr, Record to, int toAdr) {
			Record fromKey = key(from, fields, fromAdr);
			Record toKey = key(to, fields, toAdr);
			boolean unique = (mode == Mode.KEY ||
					(mode == Mode.UNIQUE && ! isEmptyKey(toKey)));
			return btree.update(fromKey, toKey, unique);
		}

		boolean add(Record rec, int adr) {
			Record key = key(rec, fields, adr);
			boolean unique = (mode == Mode.KEY ||
					(mode == Mode.UNIQUE && ! isEmptyKey(key)));
			return btree.add(key, unique);
		}

		boolean remove(Record rec, int adr) {
			Record key = key(rec, fields, adr);
			return btree.remove(key);
		}

		int getKeyAdr(Record rec) {
			return btree.get(searchKey(rec));
		}

		Record searchKey(Record rec) {
			return new RecordBuilder().addFields(rec, fields).build();
		}

		void fkeyHandleAdd(Record rec) {
		}

		void fkeyHandleRemove(Record rec) {
		}

		void fkeyHandleUpdate(Record oldrec, Record newrec) {
		}
	}

	private static class AnIndexWithFkeys extends AnIndex {
		final UpdateTransaction t;
		final ForeignKeySource fksrc;
		final Set<ForeignKeyTarget> fkdsts;

		AnIndexWithFkeys(UpdateTransaction t, Btree btree, Mode mode,
				int[] fields, String columns,
				ForeignKeySource fksrc, Set<ForeignKeyTarget> fkdsts) {
			super(btree, mode, fields, columns);
			this.t = t;
			this.fksrc = fksrc;
			if (fkdsts == null)
				this.fkdsts = Collections.emptySet();
			else
				this.fkdsts = fkdsts;
		}

		@Override
		void fkeyHandleAdd(Record rec) {
			fkeyAddBlock(searchKey(rec), "add");
		}

		void fkeyAddBlock(Record key, String action) {
			if (fksrc == null || fksrc.mode != Fkmode.BLOCK)
				return;
			if (isEmptyKey(key))
				return;
			Table fktbl = t.getTable(fksrc.tablename);
			if (fktbl == null ||
					! t.exists(fktbl.num, fktbl.namesToNums(fksrc.columns), key))
				throw new RuntimeException(action + " record blocked by foreign key to "
						+ fksrc.tablename + " " + key);
		}

		@Override
		public void fkeyHandleRemove(Record rec) {
			if (fkdsts == null)
				return;
			Record key = searchKey(rec);
			if (isEmptyKey(key))
				return;
			for (ForeignKeyTarget fk : fkdsts) {
				fkeyRemoveBlock(fk, key, "remove");
				fkeyCascadeDelete(fk, key);
			}
		}

		void fkeyRemoveBlock(ForeignKeyTarget fk, Record key, String action) {
			if (isEmptyKey(key))
				return;
			if (fk.mode == Fkmode.BLOCK &&
					t.exists(fk.tblnum, fk.colNums, key))
				throw new RuntimeException(action +
						" record blocked by foreign key in " + fk.tablename);
		}

		private void fkeyCascadeDelete(ForeignKeyTarget fk, Record key) {
			if ((fk.mode & Fkmode.CASCADE_DELETES) != 0)
				t.removeAll(fk.tblnum, fk.colNums, key);
		}

		@Override
		void fkeyHandleUpdate(Record oldrec, Record newrec) {
			Record oldkey = searchKey(oldrec);
			Record newkey = searchKey(newrec);
			if (oldkey.equals(newkey))
				return;
			fkeyAddBlock(newkey, "update(add)");
			for (ForeignKeyTarget fk : fkdsts) {
				fkeyRemoveBlock(fk, oldkey, "update(remove)");
				fkeyCascadeUpdate(fk, oldkey, newkey);
			}
		}

		private void fkeyCascadeUpdate(ForeignKeyTarget fk, Record oldkey, Record newkey) {
			if ((fk.mode & Fkmode.CASCADE_UPDATES) != 0)
				t.updateAll(fk.tblnum, fk.colNums, oldkey, newkey);
		}

		private static boolean isEmptyKey(Record key) {
			for (int i = 0; i < key.size(); ++i)
				if (key.fieldLength(i) != 0)
					return false;
			return true;
		}
	}

	static Record key(Record rec, int[] fields, int adr) {
		return new RecordBuilder().addFields(rec, fields).adduint(adr).build();
	}

	/** ignores final address field */
	static boolean isEmptyKey(Record key) {
		assert key.size() > 1;
		for (int i = 0; i < key.size() - 1; ++i)
			if (key.fieldLength(i) != 0)
				return false;
		return true;
	}

}
