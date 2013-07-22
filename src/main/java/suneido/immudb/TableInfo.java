/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.util.Arrays;

import suneido.immudb.Bootstrap.TN;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * Table stats - nextfield, nrows, totalsize, indexInfo
 * <p>
 * Semi-immutable:
 * loaded => immutable => with => mutable => store => immutable
 * <p>
 * Only mutable within a thread confined transaction.
 * <p>
 * Unlike cSuneido, table and index info is not kept in the system tables
 * since that requires more expensive updates.
 * <p>
 * Index info is stored one after another following the table info
 * in the same record.
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

	TableInfo(TableInfo ti, ImmutableList<IndexInfo> indexInfo) {
		this(ti.tblnum, ti.nextfield, ti.nrows, ti.totalsize, indexInfo);
	}

	/** When a table is dropped, its table info is replaced by an empty entry */
	static TableInfo empty(int tblnum) {
		return new TableInfo(tblnum, 0, 0, 0, ImmutableList.<IndexInfo>of());
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
			nrows += nr;
			totalsize += size;
			return this;
		}
	}

	/** @return the address of the stored record */
	int store(Storage stor) {
		if (! stored()) {
			RecordBuilder rb = new RecordBuilder();
			rb.add(tblnum).add(nextfield).add(nrows).add(totalsize);
			for (IndexInfo info : indexInfo)
				info.addToRecord(rb);
			DataRecord r = rb.build();
			r.tblnum(TN.TABLES);
			adr = r.store(stor);
		}
		return adr;
	}

	IndexInfo getIndex(int[] indexColumns) {
		for (IndexInfo ii : indexInfo)
			if (Arrays.equals(ii.columns, indexColumns))
				return ii;
		return null;
	}

	void check() {
		for (IndexInfo ii : indexInfo)
			ii.check();
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

}
