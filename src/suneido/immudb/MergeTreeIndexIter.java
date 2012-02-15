/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.immudb.BtreeNode.adr;
import suneido.intfc.database.IndexIter;
import suneido.intfc.database.Record;
import suneido.util.MergeTree;

/**
 * Adapts a MergeTree Iter to the IndexIter interface.
 */
public class MergeTreeIndexIter implements IndexIter {
	private final MergeTree<Record>.Iter mti;
	private Record cur;

	public MergeTreeIndexIter(MergeTree<Record>.Iter mti) {
		this.mti = mti;
	}

	@Override
	public boolean eof() {
		return cur == null;
	}

	@Override
	public Record curKey() {
		return cur;
	}

	@Override
	public int keyadr() {
		return adr((suneido.immudb.Record) cur);
	}

	@Override
	public void next() {
		cur = mti.next();
	}

	@Override
	public void prev() {
		cur = mti.prev();
	}

}
