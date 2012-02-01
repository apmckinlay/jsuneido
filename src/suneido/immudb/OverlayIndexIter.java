/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import suneido.intfc.database.IndexIter;
import suneido.intfc.database.Record;

/**
 * Extends MergeIndexIter to handle deletes.
 * Inserts are recorded as ProjectRecord's.
 * Actual keys are unique (because they contain the data record address)
 * So a duplicate is assumed to be a delete
 * And you only have to peek ahead one record to check for a delete
 */
public class OverlayIndexIter extends MergeIndexIter {

	public OverlayIndexIter(IndexIter global, IndexIter local) {
		// put local second to maintain default insertion order
		super(global, local);
	}

	@Override
	public void next() {
		Record cur;
		do {
			super.next();
			if (eof())
				return;
			cur = super.curKey();
			if (cur.equals(peekNext())) {
				super.next();
				cur = null;
			}
		} while (cur == null);
	}

	@Override
	public void prev() {
		Record cur;
		do {
			super.prev();
			if (eof())
				return;
			cur = super.curKey();
			if (cur.equals(peekPrev())) {
				super.prev();
				cur = null;
			}
		} while (cur == null);
	}

}
