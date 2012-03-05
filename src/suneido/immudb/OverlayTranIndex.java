/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import suneido.intfc.database.IndexIter;

/**
 * Interface between UpdateTransaction and database indexes.
 * Any writes go to a local index which is later merged with the master.
 * The global btree is read-only.
 * Deletes from the global btree are recorded by adding the key to the local btree.
 * Keys recording deletes will have a real address,
 * whereas actual new keys will have intref addresses.
 */
class OverlayTranIndex implements TranIndex {
	private final Btree2 global;
	private final Btree2 local;

	OverlayTranIndex(Btree2 global, Btree2 local) {
		this.global = global;
		this.local = local;
	}

	@Override
	public boolean add(Record key, boolean unique) {
		if (unique && get(Btree2.withoutAddress(key)) != 0)
			return false;
		return local.add(key, false);
	}

	@Override
	public int get(Record key) {
		int adr = local.get(key);
		if (adr != 0)
			return IntRefs.isIntRef(adr) ? adr : 0;
		return global.get(key);
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
		if (! local.remove(key))
			// global, so add a delete record to local
			return local.add(key, false);
		return true;
	}

	@Override
	public TranIndex.Iter iterator() {
		return new OverlayIndexIter(global, local);
	}

	@Override
	public TranIndex.Iter iterator(Record key) {
		return new OverlayIndexIter(global, local, key, key);
	}

	@Override
	public TranIndex.Iter iterator(Record org, Record end) {
		return new OverlayIndexIter(global, local, org, end);
	}

	@Override
	public TranIndex.Iter iterator(IndexIter iter) {
		return new OverlayIndexIter(global, local, (OverlayIndexIter) iter);
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

	Btree2 local() {
		return local;
	}

	@Override
	public void check() {
		throw new UnsupportedOperationException();
	}

}
