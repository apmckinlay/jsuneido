/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import suneido.intfc.database.IndexIter;

/**
 * Interface between UpdateTransaction and database indexes.
 * Read-only access goes directly to the Btree2.
 * Any writes require creating a local index which is merged with the Btree2
 * using MergeIndexIter.
 * The global btree is read-only.
 * Deletes from the global btree are recorded by adding the key to the local btree.
 * Keys recording deletes will have a real address,
 * whereas actual new keys will have intref addresses.
 */
class OverlayTranIndex implements TranIndex {
	private final Btree2 global;
	private final Btree2 local;

	OverlayTranIndex(Tran tran, Btree2 global, Btree2 local) {
		this.global = global;
		this.local = local;
	}

	@Override
	public boolean add(Record key, boolean unique) {
		if (unique && global.get(key) != 0)
			return false;
		return local.add(key, unique);
	}

	@Override
	public int get(Record key) {
		int adr = local.get(key);
		if (adr == 0 || ! IntRefs.isIntRef(adr))
			return 0;
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
		return new OverlayIndexIter(
				global.iterator(), local.iterator());
	}

	@Override
	public TranIndex.Iter iterator(Record key) {
		return new OverlayIndexIter(
				global.iterator(key), local.iterator(key));
	}

	@Override
	public TranIndex.Iter iterator(Record org, Record end) {
		return new OverlayIndexIter(
				global.iterator(org, end), local.iterator(org, end));
	}

	@Override
	public TranIndex.Iter iterator(IndexIter iter) {
		return new OverlayIndexIter(
				global.iterator(iter), local.iterator(iter));
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
		// TODO Auto-generated method stub
		return null;
	}

	Btree2 local() {
		return local;
	}

}
