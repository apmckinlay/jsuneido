/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * Table stats - nextfield, nrows, totalsize, indexInfo
 * <p>
 * Immutable when loaded or stored.
 * Mutable within a transaction.
 */
class TableInfo extends DbHashTrie.Entry {
	private int adr;
	final int tblnum;
	final int nextfield;
	private int nrows;
	private long totalsize;
	final ImmutableList<IndexInfo> indexInfo;

	TableInfo(int tblnum, int nextfield, int nrows, long totalsize,
			ImmutableList<IndexInfo> indexInfo) {
		adr = 0;
		this.tblnum = tblnum;
		this.nextfield = nextfield;
		this.nrows = nrows;
		this.totalsize = totalsize;
		this.indexInfo = indexInfo;
	}

	TableInfo(Record rec, int adr) {
		this.adr = adr;
		int i = 0;
		tblnum = rec.getInt(i++);
		nextfield = rec.getInt(i++);
		nrows = rec.getInt(i++);
		totalsize = rec.getLong(i++);
		ImmutableList.Builder<IndexInfo> list = ImmutableList.builder();
		for (; i < rec.size(); i += IndexInfo.NFIELDS)
			list.add(new IndexInfo(rec, i));
		indexInfo = list.build();
	}

	@Override
	int key() {
		return tblnum;
	}

	@Override
	int value() {
		return adr;
	}

	int nrows() {
		return nrows;
	}

	long totalsize() {
		return totalsize;
	}

	private boolean stored() {
		return adr != 0;
	}

	TableInfo with(int nr, int size) {
		if (stored())
			return new TableInfo(tblnum,
					nextfield, nrows + nr, totalsize + size, indexInfo);
		else {
			++nrows;
			totalsize += size;
			return this;
		}
	}

	int store(Storage stor) {
		if (! stored()) {
			RecordBuilder rb = new RecordBuilder();
			rb.add(tblnum).add(nextfield).add(nrows).add(totalsize);
			for (IndexInfo info : indexInfo)
				info.addToRecord(rb);
			adr = rb.build().store(stor);
		}
		return adr;
	}

	IndexInfo getIndex(String indexColumns) {
		for (IndexInfo ii : indexInfo)
			if (ii.columns.equals(indexColumns))
				return ii;
		return null;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("tblnum", tblnum)
			.add("nextfield", nextfield)
			.add("nrows", nrows)
			.add("totalsize", totalsize)
			.addValue(Iterables.toString(indexInfo))
			.toString();
	}

	void check() {
		for (IndexInfo ii : indexInfo)
			ii.check();
	}

}
