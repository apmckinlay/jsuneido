/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.util.List;

import suneido.immudb.Bootstrap.TN;
import suneido.immudb.IndexedData.Mode;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;

class Index implements Comparable<Index> {
	static final int TBLNUM = 0, COLUMNS = 1, KEY = 2,
			FKTABLE = 3, FKCOLUMNS = 4, FKMODE = 5;
	static final int BLOCK = 0, CASCADE_UPDATES = 1,
			CASCADE_DELETES = 2, CASCADE = 3;
	static final String UNIQUE = "u";
	final int tblnum;
	final int[] colNums;
	final boolean isKey;
	final boolean unique;
	final ForeignKey fksrc;

	Index(int tblnum, int[] colNums, boolean key, boolean unique) {
		this.tblnum = tblnum;
		this.colNums = colNums;
		this.isKey = key;
		this.unique = unique;
		fksrc = null;
	}

	Index(Record record) {
		this.tblnum = record.getInt(TBLNUM);
		this.colNums = convert(record.getString(COLUMNS));
		Object key = record.get(KEY); // key is false, true, or "u"
		this.isKey = key == Boolean.TRUE;
		this.unique = key.equals(UNIQUE);
		fksrc = get_fksrc(record);
	}

	private static final int[] noColumns = new int[0];
	private static final CharMatcher cm = CharMatcher.is(',');
	private static final Splitter splitter = Splitter.on(',');

	int[] convert(String s) {
		if (s.equals(""))
			return noColumns;
		int[] cols = new int[cm.countIn(s) + 1];
		int i = 0;
		for (String c : splitter.split(s))
			cols[i++] = Integer.parseInt(c);
		return cols;
	}

	private ForeignKey get_fksrc(Record record) {
		String fktable = record.getString(FKTABLE);
		if (!fktable.equals(""))
			return new ForeignKey(fktable, record.getString(FKCOLUMNS),
					record.getInt(FKMODE));
		return null;
	}

	Record toRecord() {
		return toRecord(tblnum, colNumsString(), isKey, unique, fksrc);
	}

	private String colNumsString() {
		return Ints.join(",", colNums);
	}

	List<String> columns(List<String> fields) {
		ImmutableList.Builder<String> cols = ImmutableList.builder();
		for (int i : colNums)
			cols.add(fields.get(i));
		return cols.build();
	}

	Mode mode() {
		return isKey ? Mode.KEY : unique ? Mode.UNIQUE : Mode.DUPS;
	}

	static Record toRecord(int tblnum, String columns, boolean isKey,
			boolean unique, ForeignKey fksrc) {
		RecordBuilder rb = new RecordBuilder();
		rb.add(tblnum).add(columns);
		if (unique)
			rb.add(UNIQUE);
		else
			rb.add(isKey);
		if (fksrc != null)
			rb.add(fksrc.tablename).add(fksrc.columns).add(fksrc.mode);
		Record r = rb.build();
		r.tblnum = TN.INDEXES;
		return r;
	}

//	private ImmutableList<ForeignKey> get_fkdsts(List<Record> fkdstrecs) {
//		ImmutableList.Builder<ForeignKey> builder = ImmutableList.builder();
//		for (Record ri : fkdstrecs)
//			builder.add(new ForeignKey(ri.getInt(TBLNUM),
//					ri.getString(COLUMNS),
//					ri.getInt(FKMODE)));
//		return builder.build();
//	}

//	static String getColumns(Record r) {
//		String columns = r.getString(COLUMNS);
//		return columns;
//	}

	boolean isKey() {
		return isKey;
	}

	String schema(StringBuilder sb, Columns cols) {
		if (isKey)
			sb.append(" key");
		else
			sb.append(" index").append(unique ? " unique" : "");
		sb.append("(").append(cols.names(colNums)).append(")");
//		if (index.fksrc != null && !index.fksrc.tablename.equals("")) {
//			sb.append(" in ").append(index.fksrc.tablename);
//			if (!index.fksrc.columns.equals(index.columns))
//				sb.append("(").append(index.fksrc.columns).append(")");
//			if (index.fksrc.mode == Index.CASCADE)
//				sb.append(" cascade");
//			else if (index.fksrc.mode == Index.CASCADE_UPDATES)
//				sb.append(" cascade update");
		return sb.toString();
	}

	@Override
	public int compareTo(Index that) {
		return this.colNumsString().compareTo(that.colNumsString());
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (! (other instanceof Index))
			return false;
		Index that = (Index) other;
		return this.colNums.equals(that.colNums);
	}

	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return (isKey() ? "key" : "index") + (unique ? "unique" : "") +
				"(" + Ints.join(",", colNums) + ")";
	}

}
