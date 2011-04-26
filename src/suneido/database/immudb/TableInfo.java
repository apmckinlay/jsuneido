/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

@Immutable
public class TableInfo extends DbHashTrie.Entry {
	public final int tblnum;
	public final int nextfield;
	public final int nrows;
	public final long totalsize;
	public final ImmutableList<IndexInfo> indexInfo;

	public TableInfo(int tblnum, int nextfield, int nrows, long totalsize,
			ImmutableList<IndexInfo> indexInfo) {
		this.tblnum = tblnum;
		this.nextfield = nextfield;
		this.nrows = nrows;
		this.totalsize = totalsize;
		this.indexInfo = indexInfo;
	}

	public TableInfo(Record rec) {
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
	public int key() {
		return tblnum;
	}

	public Record toRecord() {
		RecordBuilder rb = new RecordBuilder();
		rb.add(tblnum).add(nextfield).add(nrows).add(totalsize);
		for (IndexInfo info : indexInfo)
			info.addToRecord(rb);
		return rb.build();
	}

	public static Record toRecord(int tblnum, int nextfield, int nrows, long totalsize,
			String indexColumns, BtreeInfo btreeInfo) {
		RecordBuilder rb = new RecordBuilder();
		rb.add(tblnum).add(nextfield).add(nrows).add(totalsize);
		IndexInfo.addToRecord(rb, indexColumns, btreeInfo);
		return rb.build();
	}

	public IndexInfo firstIndex() {
		return indexInfo.get(0);
	}

	public IndexInfo getIndex(String indexColumns) {
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

}