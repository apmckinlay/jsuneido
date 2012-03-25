/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import gnu.trove.set.hash.TIntHashSet;

/**
 * Extends MergeIndexIter to handle deletes.
 * Inserts are recorded as ProjectRecord's.
 * Actual keys are unique (because they contain the data record address)
 * So a duplicate is assumed to be a delete
 * And you only have to peek ahead one record to check for a delete
 */
public class OverlayIndexIter extends MergeIndexIter {
	private final TIntHashSet deletes;
	private IndexRange ir;
	private final Record from;
	private final Record to;

	OverlayIndexIter(TranIndex global, TranIndex local, TIntHashSet deletes) {
		super(global.iterator(), local.iterator());
		this.deletes = deletes;
		from = DatabasePackage2.MIN_RECORD;
		to = DatabasePackage2.MAX_RECORD;
	}

	/** for tests */
	OverlayIndexIter(TranIndex.Iter iter1, TranIndex.Iter iter2, TIntHashSet deletes) {
		super(iter1, iter2);
		this.deletes = deletes;
		from = DatabasePackage2.MIN_RECORD;
		to = DatabasePackage2.MAX_RECORD;
		ir = new IndexRange();
	}

	OverlayIndexIter(TranIndex global, TranIndex local, TIntHashSet deletes,
			Record from, Record to) {
		super(global.iterator(from, to), local.iterator(from, to));
		this.deletes = deletes;
		this.from = from;
		this.to = to;
	}

	/** copy constructor, used for Cursor set transaction */
	OverlayIndexIter(TranIndex global, TranIndex local, TIntHashSet deletes,
			OverlayIndexIter iter) {
		super(global.iterator(iter.iter1), local.iterator(iter.iter2), iter);
		this.deletes = deletes;
		from = iter.from;
		to = iter.to;
	}

	void trackRange(IndexRange ir) {
		this.ir = ir;
	}

	@Override
	public void next() {
		if (eof())
			return;
		if (rewound)
			ir.lo = from;
		do {
			super.next();
			if (eof()) {
				ir.hi = to;
				return;
			}
		} while (deletes.contains(keyadr()));
		if (curKey().compareTo(ir.hi) > 0)
			ir.hi = curKey();
	}

	@Override
	public void prev() {
		if (eof())
			return;
		if (rewound)
			ir.hi = to;
		do {
			super.prev();
			if (eof()) {
				ir.lo = from;
				return;
			}
		} while (deletes.contains(keyadr()));
		if (curKey().compareTo(ir.lo) < 0)
			ir.lo = curKey();
	}

}
