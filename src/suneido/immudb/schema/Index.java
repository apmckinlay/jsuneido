/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb.schema;

import suneido.immudb.*;
import suneido.immudb.IndexedData.Mode;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.primitives.Ints;

public class Index implements Comparable<Index> {
	public static final int TBLNUM = 0, COLUMNS = 1, KEY = 2,
			FKTABLE = 3, FKCOLUMNS = 4, FKMODE = 5;
	public static final int BLOCK = 0, CASCADE_UPDATES = 1,
			CASCADE_DELETES = 2, CASCADE = 3;
	static final String UNIQUE = "u";
	public final int tblnum;
	public final int[] colNums;
	public final boolean isKey;
	public final boolean unique;
	public final ForeignKey fksrc;


	public Index(int tblnum, int[] colNums, boolean key, boolean unique) {
		this.tblnum = tblnum;
		this.colNums = colNums;
		this.isKey = key;
		this.unique = unique;
		fksrc = null;
	}

	public Index(Record record) {
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

	public int[] convert(String s) {
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

	public Record toRecord() {
		return toRecord(tblnum, colNumsString(), isKey, unique, fksrc);
	}

	public String colNumsString() {
		return Ints.join(",", colNums);
	}

	public Mode mode() {
		return isKey ? Mode.KEY : unique ? Mode.UNIQUE : Mode.DUPS;
	}

	public static Record toRecord(int tblnum, String columns, boolean isKey,
			boolean unique, ForeignKey fksrc) {
		RecordBuilder rb = new RecordBuilder();
		rb.add(tblnum).add(columns);
		if (unique)
			rb.add(UNIQUE);
		else
			rb.add(isKey);
		if (fksrc != null)
			rb.add(fksrc.tablename).add(fksrc.columns).add(fksrc.mode);
		return rb.build();
	}

//	private ImmutableList<ForeignKey> get_fkdsts(List<Record> fkdstrecs) {
//		ImmutableList.Builder<ForeignKey> builder = ImmutableList.builder();
//		for (Record ri : fkdstrecs)
//			builder.add(new ForeignKey(ri.getInt(TBLNUM),
//					ri.getString(COLUMNS),
//					ri.getInt(FKMODE)));
//		return builder.build();
//	}

//	public static String getColumns(Record r) {
//		String columns = r.getString(COLUMNS);
//		return columns;
//	}

	public boolean isKey() {
		return isKey;
	}

	@Override
	public String toString() {
		return (isKey() ? "key" : "index") + (unique ? "unique" : "") +
				"(" + Ints.join(",", colNums) + ")";
	}

	public String schema(StringBuilder sb, Columns cols) {
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

}
