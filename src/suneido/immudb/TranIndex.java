/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import suneido.immudb.Btree.Update;
import suneido.intfc.database.IndexIter;

interface TranIndex {

	boolean add(Record key, boolean unique);

	int get(Record key);

	Update update(Record oldkey, Record newkey, boolean unique);

	boolean remove(Record key);

	IndexIter iterator();

	IndexIter iterator(Record key);

	IndexIter iterator(Record org, Record end);

	IndexIter iterator(IndexIter iter);

	int totalSize();
	float rangefrac(Record from, Record to);

}