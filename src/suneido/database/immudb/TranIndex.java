/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

/**
 * Common interface for {@link Btree} and {@link OverlayIndex}
 */
interface TranIndex {

	boolean add(BtreeKey key, boolean unique);

	int get(Record key);

	Update update(BtreeKey oldkey, BtreeKey newkey, boolean unique);

	boolean remove(BtreeKey key);

	IndexIter iterator();

	IndexIter iterator(Record key);

	IndexIter iterator(Record org, Record end);

	IndexIter iterator(IndexIter iter);

	int totalSize();
	float rangefrac(Record from, Record to);

	BtreeInfo info();

	/**
	 * Used to handle "resetting" the iterator when the index is modified
	 */
	interface Iter extends IndexIter {

		boolean isRewound();

		BtreeKey cur();

		void seek(BtreeKey key);

		/**
		 * @return true if the underlying index has been modified
		 * since the iterator was constructed
		 */
		boolean isIndexModified();

	}

	enum Update { OK, NOT_FOUND, ADD_FAILED }

	void check();

}
