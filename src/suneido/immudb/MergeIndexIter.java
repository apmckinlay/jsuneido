/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import suneido.intfc.database.IndexIter;
import suneido.intfc.database.Record;

/**
 * Merges two IndexIter's.
 * In the case of duplicates, iter1 is treated as "before" iter2.
 * Hard part is switching directions.
 * Used to combine global Btree and transaction local MergeTree.
 */
public class MergeIndexIter implements IndexIter {
	private final IndexIter iter1;
	private final IndexIter iter2;
	private IndexIter curIter;
	private Record cur = null;
	private enum State { REWOUND, NEXT, PREV, START, END };
	private State state = State.REWOUND;

	public MergeIndexIter(IndexIter iter1, IndexIter iter2) {
		this.iter1 = iter1;
		this.iter2 = iter2;
	}

	@Override
	public boolean eof() {
		return cur == null;
	}

	@Override
	public void next() {
		if (state == State.END)
			return;
		if (state == State.PREV)
			curIter.next();
		if (state != State.NEXT) {
			iter1.next();
			iter2.next();
			state = State.NEXT;
		}
		if (! iter1.eof() &&
				(iter2.eof() || iter1.curKey().compareTo(iter2.curKey()) <= 0)) {
			cur = iter1.curKey();
			curIter = iter1;
			iter1.next();
		} else if (! iter2.eof()) {
			cur = iter2.curKey();
			curIter = iter2;
			iter2.next();
		} else { // eof on both
			cur = null;
			curIter = null;
			state = State.END;
		}
	}

	@Override
	public void prev() {
		if (state == State.START)
			return;
		if (state == State.NEXT)
			curIter.prev();
		if (state != State.PREV) {
			iter1.prev();
			iter2.prev();
			state = State.PREV;
		}
		if (! iter1.eof() &&
				(iter2.eof() || iter1.curKey().compareTo(iter2.curKey()) > 0)) {
			cur = iter1.curKey();
			curIter = iter1;
			iter1.prev();
		} else if (! iter2.eof()) {
			cur = iter2.curKey();
			curIter = iter2;
			iter2.prev();
		} else { // eof on both
			cur = null;
			curIter = null;
			state = State.START;
		}
	}

	protected Record peekNext() {
		if (! iter1.eof() &&
				(iter2.eof() || iter1.curKey().compareTo(iter2.curKey()) <= 0))
			return iter1.curKey();
		else if (! iter2.eof())
			return iter2.curKey();
		else // eof on both
			return null;
	}

	protected Record peekPrev() {
		if (! iter1.eof() &&
				(iter2.eof() || iter1.curKey().compareTo(iter2.curKey()) > 0))
			return iter1.curKey();
		else if (! iter2.eof())
			return iter2.curKey();
		else // eof on both
			return null;
	}

	@Override
	public Record curKey() {
		return cur;
	}

	@Override
	public int keyadr() {
		return Btree.getAddress((suneido.immudb.Record) cur);
	}

}
