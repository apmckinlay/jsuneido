/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

/* Copyright 2010 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import gnu.trove.set.hash.TIntHashSet;
import suneido.SuException;
import suneido.intfc.database.Fkmode;
import suneido.intfc.database.Transaction.Blocking;

/**
 * Coordinates index updates for a table.
 */
class IndexedData {
	enum Mode { KEY, UNIQUE, DUPS };
	private final ReadWriteTransaction t;
	private final Tran tran;
	private final List<AnIndex> indexes = new ArrayList<>();
	private TIntHashSet deletes;

	IndexedData(ReadWriteTransaction t) {
		this.t = t;
		tran = t.tran();
	}

	IndexedData setDeletes(TIntHashSet deletes) {
		this.deletes = deletes;
		return this;
	}

	/** setup method */
	IndexedData index(TranIndex btree, Mode mode, int[] colNums, String colNames,
			ForeignKeySource fksrc, Set<ForeignKeyTarget> fkdsts) {
		indexes.add(fksrc == null && fkdsts == null
				? new AnIndex(btree, mode, colNums, colNames)
				: new AnIndexWithFkeys(
						t, btree, mode, colNums, colNames, fksrc, fkdsts));
		return this;
	}

	IndexedData index(TranIndex btree, Mode mode, int[] colNums, String colNames) {
		indexes.add(new AnIndex(btree, mode, colNums, colNames));
		return this;
	}

	/** for tests (without foreign keys) */
	IndexedData index(TranIndex btree, Mode mode, int... fields) {
		indexes.add(new AnIndex(btree, mode, fields, ""));
		return this;
	}

	int add(Record rec) {
		int adr = rec.address(); // if exclusive tran it will already be saved
		if (adr == 0)
			adr = tran.refToInt(rec);
		add(rec, adr);
		return adr;
	}

	// used by TableBuilder to add indexes to existing tables
	void add(Record rec, int adr) {
		assert adr != 0;
		for (AnIndex index : indexes)
			index.fkeyHandleAdd(rec);
		for (AnIndex index : indexes)
			if (! index.add(rec, adr)) {
				// undo previous add's
				for (AnIndex idx : indexes) {
					if (idx == index)
						break;
					if (! idx.remove(rec, adr))
						t.abortThrow("possible corruption");
				}
				throw new SuException("duplicate key: " +
						index.columns + " = " + index.searchKey(rec));
			}
	}

	/** @return the address of the record */
	int remove(Record rec) {
		for (AnIndex index : indexes)
			index.fkeyHandleRemove(rec);
		int adr = rec.address();
		if (adr == 0)
			adr = firstKey().getKeyAdr(rec);
		if (adr == 0)
			throw new SuException("remove couldn't find record");
		for (AnIndex index : indexes)
			if (! index.remove(rec, adr))
				// can't undo like add, may have cascaded
				t.abortThrow("index remove failed (possible corruption?)" +
						" " + index.columns + " = " + index.searchKey(rec));
		trackRemove(adr, REMOVED);
		return adr;
	}

	/** @return the address of the from record */
	int update(Record from, Record to, Blocking blocking) {
		for (AnIndex index : indexes)
			index.fkeyHandleUpdate(from, to, blocking);

		int fromAdr = from.address();
		if (fromAdr == 0)
			fromAdr = firstKey().getKeyAdr(from);
		if (fromAdr == 0)
			throw new SuException("update couldn't find record");
		Object oldref = trackRemove(fromAdr, UPDATED);
		try {
			int toAdr = to.address();
			if (toAdr == 0) // if exclusive tran it will already be saved
				toAdr = tran.refToInt(to);
			for (AnIndex index : indexes)
				switch (index.update(from, fromAdr, to, toAdr)) {
				case NOT_FOUND:
					t.abortThrow("update failed: old record not found " +
							"(possible corruption)");
					break;
				case ADD_FAILED:
					t.abortThrow("update failed: duplicate key: " +
							index.columns + " = " + index.searchKey(to));
					break;
				case OK:
					oldref = null; // succeeded, so don't restore
					break;
				default:
					throw new SuException("unhandled update result");
				}
		} finally {
			// restore old reference if we didn't succeed
			if (oldref != null)
				tran.update(fromAdr, oldref);
		}
		return fromAdr;
	}

	static class Special { }
	static class Removed extends Special { }
	static final Removed REMOVED = new Removed();
	static class Updated extends Special { }
	static final Updated UPDATED = new Updated();

	/**
	 * If new record (intref) then updates intref
	 * to either REMOVED or UPDATED.
	 * This is used by UpdateTransaction storeData.
	 */
	private Object trackRemove(int adr, Special how) {
		Object oldref = null;
		if (IntRefs.isIntRef(adr)) {
			oldref = tran.intToRef(adr);
			tran.update(adr, how);
		} else if (deletes != null)
			deletes.add(adr);
		return oldref;
	}

	private AnIndex firstKey() {
		for (AnIndex index : indexes)
			if (index.mode == Mode.KEY)
				return index;
		throw new SuException("no key!");
	}

	private static class AnIndex {
		final TranIndex btree;
		final Mode mode;
		final int[] fields;
		final String columns;

		AnIndex(TranIndex btree, Mode mode, int[] fields, String columns) {
			this.btree = btree;
			this.mode = mode;
			this.fields = fields;
			this.columns = columns;
		}

		boolean add(Record rec, int adr) {
			BtreeKey key = key(rec, fields, adr);
			boolean unique = (mode == Mode.KEY ||
					(mode == Mode.UNIQUE && ! key.isEmptyKey()));
			return btree.add(key, unique);
		}

		boolean remove(Record rec, int adr) {
			BtreeKey key = key(rec, fields, adr);
			return btree.remove(key);
		}

		Btree.Update update(Record from, int fromAdr, Record to, int toAdr) {
			BtreeKey fromKey = key(from, fields, fromAdr);
			BtreeKey toKey = key(to, fields, toAdr);
			boolean unique = (mode == Mode.KEY ||
					(mode == Mode.UNIQUE && ! toKey.isEmptyKey()));
			return btree.update(fromKey, toKey, unique);
		}

		int getKeyAdr(Record rec) {
			return btree.get(searchKey(rec));
		}

		Record searchKey(Record rec) {
			return keyBuilder(rec, fields).arrayRec();
		}

		void fkeyHandleAdd(Record rec) {
		}

		void fkeyHandleRemove(Record rec) {
		}

		void fkeyHandleUpdate(Record oldrec, Record newrec, Blocking blocking) {
		}
	}

	private static class AnIndexWithFkeys extends AnIndex {
		final ReadWriteTransaction t;
		final ForeignKeySource fksrc;
		final Set<ForeignKeyTarget> fkdsts;

		AnIndexWithFkeys(ReadWriteTransaction t, TranIndex btree, Mode mode,
				int[] fields, String columns,
				ForeignKeySource fksrc, Set<ForeignKeyTarget> fkdsts) {
			super(btree, mode, fields, columns);
			assert t != null;
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
			if (fksrc == null || isEmptyKey(key))
				return;
			Table fksrctbl = t.getTable(fksrc.tablename);
			if (fksrctbl == null ||
					! t.exists(fksrctbl.num, fksrctbl.namesToNums(fksrc.columns), key))
				throw new SuException(action + " record blocked by foreign key to "
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
				throw new SuException(action +
						" record blocked by foreign key in " + fk.tablename);
		}

		private void fkeyCascadeDelete(ForeignKeyTarget fk, Record key) {
			if ((fk.mode & Fkmode.CASCADE_DELETES) != 0)
				t.removeAll(fk.tblnum, fk.colNums, key);
		}

		@Override
		void fkeyHandleUpdate(Record oldrec, Record newrec, Blocking blocking) {
			Record oldkey = searchKey(oldrec);
			Record newkey = searchKey(newrec);
			if (oldkey.equals(newkey))
				return;
			if (blocking == Blocking.BLOCK)
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

	}

	static boolean isEmptyKey(Record key) {
		for (int i = 0; i < key.size(); ++i)
			if (key.fieldLength(i) != 0)
				return false;
		return true;
	}

	static BtreeKey key(Record rec, int[] fields, int adr) {
		return keyBuilder(rec, fields).btreeKey(adr);
	}

	private static RecordBuilder keyBuilder(Record rec, int[] fields) {
		RecordBuilder rb = new RecordBuilder();
		for (int f : fields)
			if (f >= 0)
				rb.add(rec, f);
			else
				//TODO handle other transforms
				rb.add(rec.getString(-f - 2).toLowerCase());
		return rb;
	}

}
