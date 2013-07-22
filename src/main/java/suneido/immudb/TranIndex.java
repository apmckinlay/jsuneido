/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import suneido.intfc.database.IndexIter;

/**
 * Common interface for {@link Btree} and {@link OverlayIndex}
 */
interface TranIndex {

	boolean add(BtreeKey key, boolean unique);

	int get(Record key);

	Update update(BtreeKey oldkey, BtreeKey newkey, boolean unique);

	boolean remove(BtreeKey key);

	Iter iterator();

	Iter iterator(Record key);

	Iter iterator(Record org, Record end);

	Iter iterator(IndexIter iter);

	int totalSize();
	float rangefrac(Record from, Record to);

	BtreeInfo info();

	interface Iter extends IndexIter {

		@Override
		Record curKey();

		void rewind();

	}

	/**
	 * Used to handle "resetting" the iterator when the index is modified
	 */
	interface IterPlus extends Iter {

		boolean isRewound();

		BtreeKey cur();

		BtreeKey oldNext();

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