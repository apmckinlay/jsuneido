/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

/**
 * Wraps a TranIndex.Iter and handles "resetting" it
 * when the underlying index is modified
 */
public class IndexIter {
	private final TranIndex.IterPlus iter;

	IndexIter(TranIndex.IterPlus iter) {
		this.iter = iter;
	}

	public boolean eof() {
		return iter.eof();
	}

	public int keyadr() {
		return iter.keyadr();
	}

	public void next() {
		if (eof())
			return;
		if (! iter.isRewound() && iter.isIndexModified())
			iter.seek(iter.oldNext());
		else
			iter.next();
	}

	public void prev() {
		if (eof())
			return;
		if (! iter.isRewound() && iter.isIndexModified())
			iter.seek(iter.cur());
		iter.prev();
	}

	public Record curKey() {
		return iter.curKey();
	}

	public void rewind() {
		iter.rewind();
	}

}
