/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

/**
 * Wraps a TranIndex.Iter and handles "resetting" it
 * when the underlying index is modified
 */
public class IndexIter implements TranIndex.Iter {
	private final TranIndex.IterPlus iter;

	IndexIter(TranIndex.IterPlus iter) {
		this.iter = iter;
	}

	@Override
	public boolean eof() {
		return iter.eof();
	}

	@Override
	public int keyadr() {
		return iter.keyadr();
	}

	@Override
	public void next() {
		if (eof())
			return;
		if (! iter.isRewound() && iter.isIndexModified())
			iter.seek(iter.oldNext());
		else
			iter.next();
	}

	@Override
	public void prev() {
		if (eof())
			return;
		if (! iter.isRewound() && iter.isIndexModified())
			iter.seek(iter.cur());
		iter.prev();
	}

	@Override
	public Record curKey() {
		return iter.curKey();
	}

	@Override
	public void rewind() {
		iter.rewind();
	}

}
