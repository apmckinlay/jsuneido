/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import suneido.intfc.database.IndexIter;

interface TranIndex {

	boolean add(Record key, boolean unique);

	int get(Record key);

	Update update(Record oldkey, Record newkey, boolean unique);

	boolean remove(Record key);

	Iter iterator();

	Iter iterator(Record key);

	Iter iterator(Record org, Record end);

	Iter iterator(IndexIter iter);

	int totalSize();
	float rangefrac(Record from, Record to);

	BtreeInfo info();

	interface Iter extends IndexIter {

		@Override
		public Record curKey();

	}

	enum Update { OK, NOT_FOUND, ADD_FAILED }

	void check();

}