/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import suneido.database.query.Query.Dir;
import suneido.immudb.TranIndex.Iter;

/**
 * Merges two IndexIter's.
 * In the case of duplicates, iter1 is treated as "before" iter2.
 * Hard part is switching directions.
 * Used to combine global index and transaction local index.
 */
class MergeIndexIter implements TranIndex.Iter {
	protected final TranIndex.Iter iter1;
	protected final TranIndex.Iter iter2;
	protected boolean rewound = true;
	private TranIndex.Iter curIter;
	private Dir lastDir;

	public MergeIndexIter(TranIndex.Iter iter1, TranIndex.Iter iter2) {
		this.iter1 = iter1;
		this.iter2 = iter2;
	}

	public MergeIndexIter(TranIndex.Iter iter1, TranIndex.Iter iter2, MergeIndexIter iter) {
		this.iter1 = iter1;
		this.iter2 = iter2;
		if (iter.curIter == iter.iter1)
			curIter = iter1;
		else if (iter.curIter == iter.iter2)
			curIter = iter2;
		else
			assert iter.curIter == null;
		assert eof() == iter.eof();
		rewound = iter.rewound;
		lastDir = iter.lastDir;
	}

	@Override
	public boolean eof() {
		return curIter != null && curIter.eof();
	}

	@Override
	public void next() {
		if (eof())
			return;
		if (rewound) {
			iter1.next();
			iter2.next();
			rewound = false;
		} else if (lastDir == Dir.NEXT)
			curIter.next();
		else { // switched direction
			next(iter1);
			next(iter2);
		}
		curIter = minIter();
		lastDir = Dir.NEXT;
	}

	@Override
	public void prev() {
		if (eof())
			return;
		if (rewound) {
			iter1.prev();
			iter2.prev();
			rewound = false;
		} else if (lastDir == Dir.PREV) {
			curIter.prev();
		} else { // switched direction
			prev(iter1);
			prev(iter2);
		}
		curIter = maxIter();
		lastDir = Dir.PREV;
	}

	private static void next(TranIndex.Iter iter) {
		if (iter.eof())
			iter.rewind();
		iter.next();
	}

	private static void prev(TranIndex.Iter iter) {
		if (iter.eof())
			iter.rewind();
		iter.prev();
	}

	private Iter minIter() {
		if (iter1.eof())
			return iter2;
		else if (iter2.eof())
			return iter1;
		return iter1.curKey().compareTo(iter2.curKey()) <= 0 ? iter1 : iter2;
	}

	private Iter maxIter() {
		if (iter1.eof())
			return iter2;
		else if (iter2.eof())
			return iter1;
		return iter1.curKey().compareTo(iter2.curKey()) > 0 ? iter1 : iter2;
	}

	@Override
	public Record curKey() {
		return curIter.curKey();
	}

	@Override
	public int keyadr() {
		return curIter.keyadr();
	}

	@Override
	public void rewind() {
		rewound = true;
		curIter = null;
		iter1.rewind();
		iter2.rewind();
		lastDir = null;
	}

}
