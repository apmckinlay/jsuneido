/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

/**
 * Extends MergeIndexIter to handle deletes.
 * Inserts are recorded as ProjectRecord's.
 * Actual keys are unique (because they contain the data record address)
 * So a duplicate is assumed to be a delete
 * And you only have to peek ahead one record to check for a delete
 */
public class OverlayIndexIter extends MergeIndexIter {
	private IndexRange ir;
	private final Record from;
	private final Record to;

	OverlayIndexIter(TranIndex global, TranIndex local) {
		super(global.iterator(), local.iterator());
		from = DatabasePackage2.MIN_RECORD;
		to = DatabasePackage2.MAX_RECORD;
	}

	/** for tests */
	OverlayIndexIter(TranIndex.Iter iter1, TranIndex.Iter iter2) {
		super(iter1, iter2);
		from = DatabasePackage2.MIN_RECORD;
		to = DatabasePackage2.MAX_RECORD;
		ir = new IndexRange();
	}

	OverlayIndexIter(TranIndex global, TranIndex local, Record from, Record to) {
		super(global.iterator(from, to), local.iterator(from, to));
		this.from = from;
		this.to = to;
	}

	OverlayIndexIter(Btree2 global, Btree2 local, OverlayIndexIter iter) {
		super(global.iterator(iter), local.iterator(iter));
		from = iter.from;
		to = iter.to;
	}

	void trackRange(IndexRange ir) {
		this.ir = ir;
	}

	@Override
	public void next() {
		if (state == State.REWOUND)
			ir.lo = from;
		Record cur;
		do {
			super.next();
			if (eof()) {
				ir.hi = to;
				return;
			}
			cur = super.curKey();
			if (cur.equals(peekNext())) {
				super.next();
				cur = null;
			}
		} while (cur == null);
		if (cur.compareTo(ir.hi) > 0)
			ir.hi = cur;
	}

	@Override
	public void prev() {
		if (state == State.REWOUND)
			ir.hi = to;
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
		if (cur.compareTo(ir.lo) < 0)
			ir.lo = cur;
	}

}
