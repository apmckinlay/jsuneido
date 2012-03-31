/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;

import suneido.intfc.database.IndexIter;

import com.google.common.collect.Lists;

/**
 * Interface between UpdateTransaction and database indexes.
 * Any writes go to a local index which is later merged with the master.
 * The global btree is read-only.
 * Deletes from the global btree are recorded by adding the key to the local btree.
 * Keys recording deletes will have a real address,
 * whereas actual new keys will have intref addresses.
 * The delete entries are used by UpdateTransaction updateBtrees
 */
class OverlayIndex implements TranIndex {
	private final Btree global;
	private final Btree local;
	/** a reference to the transaction deletes */
	private final TIntHashSet deletes;
	final ArrayList<Record> removedKeys = Lists.newArrayList();

	OverlayIndex(Btree global, Btree local, TIntHashSet deletes) {
		this.global = global;
		this.local = local;
		this.deletes = deletes;
	}

	@Override
	public boolean add(Record key, boolean unique) {
		if (unique && get(Btree.withoutAddress(key)) != 0)
			return false;
		return local.add(key, false);
	}

	/** WARNING: assumes keys are unique excluding address */
	@Override
	public int get(Record key) {
		// check local first to handle where global was deleted and local added
		int adr = local.get(key);
		if (adr != 0)
			return adr;
		adr = global.get(key);
		return adr == 0 || deletes.contains(adr) ? 0 : adr;
	}

	@Override
	public Update update(Record oldkey, Record newkey, boolean unique) {
		if (! remove(oldkey))
			return Update.NOT_FOUND;
		if (! add(newkey, unique))
			return Update.ADD_FAILED;
		return Update.OK;
	}

	@Override
	public boolean remove(Record key) {
		if (IntRefs.isIntRef(BtreeNode.adr(key)))
			return local.remove(key);
		else { // global
			if (global.get(key) == 0)
				return false;
			removedKeys.add(key);
			return true;
		}
	}

	@Override
	public TranIndex.Iter iterator() {
		return new Iter(global, local, deletes);
	}

	@Override
	public TranIndex.Iter iterator(Record key) {
		return new Iter(global, local, deletes, key, key);
	}

	@Override
	public TranIndex.Iter iterator(Record org, Record end) {
		return new Iter(global, local, deletes, org, end);
	}

	@Override
	public TranIndex.Iter iterator(IndexIter iter) {
		return new Iter(global, local, deletes, (Iter) iter);
	}

	@Override
	public int totalSize() {
		return global.totalSize();
	}

	@Override
	public float rangefrac(Record from, Record to) {
		return global.rangefrac(from, to);
	}

	@Override
	public BtreeInfo info() {
		throw new UnsupportedOperationException();
	}

	Btree local() {
		return local;
	}

	@Override
	public void check() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Extends MergeIndexIter to handle deletes.
	 * Inserts are recorded as ProjectRecord's.
	 * Actual keys are unique (because they contain the data record address)
	 * So a duplicate is assumed to be a delete
	 * And you only have to peek ahead one record to check for a delete
	 */
	static class Iter extends MergeIndexIter {
		private final TIntHashSet deletes;
		private IndexRange ir;
		private final Record from;
		private final Record to;

		Iter(TranIndex global, TranIndex local, TIntHashSet deletes) {
			super(global.iterator(), local.iterator());
			this.deletes = deletes;
			from = DatabasePackage.MIN_RECORD;
			to = DatabasePackage.MAX_RECORD;
		}

		/** for tests */
		Iter(TranIndex.Iter iter1, TranIndex.Iter iter2, TIntHashSet deletes) {
			super(iter1, iter2);
			this.deletes = deletes;
			from = DatabasePackage.MIN_RECORD;
			to = DatabasePackage.MAX_RECORD;
			ir = new IndexRange();
		}

		Iter(TranIndex global, TranIndex local, TIntHashSet deletes,
				Record from, Record to) {
			super(global.iterator(from, to), local.iterator(from, to));
			this.deletes = deletes;
			this.from = from;
			this.to = to;
		}

		/** copy constructor, used for Cursor set transaction */
		Iter(TranIndex global, TranIndex local, TIntHashSet deletes, Iter iter) {
			super(global.iterator(iter.iter1), local.iterator(iter.iter2), iter);
			this.deletes = deletes;
			from = iter.from;
			to = iter.to;
		}

		void trackRange(IndexRange ir) {
			this.ir = ir;
		}

		@Override
		public void next() {
			if (eof())
				return;
			if (rewound)
				ir.lo = from;
			do {
				super.next();
				if (eof()) {
					ir.hi = to;
					return;
				}
			} while (deletes.contains(keyadr()));
			if (curKey().compareTo(ir.hi) > 0)
				ir.hi = curKey();
		}

		@Override
		public void prev() {
			if (eof())
				return;
			if (rewound)
				ir.hi = to;
			do {
				super.prev();
				if (eof()) {
					ir.lo = from;
					return;
				}
			} while (deletes.contains(keyadr()));
			if (curKey().compareTo(ir.lo) < 0)
				ir.lo = curKey();
		}
	} // end of Iter

}
