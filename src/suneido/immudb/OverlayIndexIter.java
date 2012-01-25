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
 *
 * So you only have to peek ahead one record to check for a delete
 */
public class OverlayIndexIter extends MergeIndexIter {
	private Record cur;

	public OverlayIndexIter(IndexIter global, IndexIter local) {
		// put global first so local delete of global record has delete second
		// to match local delete of local record
		super(global, local);
	}

	@Override
	public void next() {
		do {
			super.next();
			if (eof())
				return;
			cur = super.curKey();
			if (cur.equals(peekNext())) {
				super.next();
				assert ! (super.curKey() instanceof ProjectRecord); // delete
				cur = null;
			}
		} while (cur == null);
	}

	@Override
	public Record curKey() {
		return cur;
	}

}
