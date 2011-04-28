/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb.schema;


import suneido.database.immudb.Record;
import suneido.database.immudb.RecordBuilder;

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
	public final int[] columns;
	public final boolean isKey;
	public final boolean unique;
	public final ForeignKey fksrc;


	public Index(int tblnum, int[] columns, boolean key, boolean unique) {
		this.tblnum = tblnum;
		this.columns = columns;
		this.isKey = key;
		this.unique = unique;
		fksrc = null;
	}

	public Index(Record record) {
		this.tblnum = record.getInt(TBLNUM);
		this.columns = convert(record.getString(COLUMNS));
		Object key = record.get(KEY); // key is false, true, or "u"
		this.isKey = key == Boolean.TRUE;
		this.unique = key.equals(UNIQUE);
		fksrc = get_fksrc(record);
	}

	private static final CharMatcher cm = CharMatcher.is(',');
	private static final Splitter splitter = Splitter.on(',');

	public int[] convert(String s) {
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
		return toRecord(tblnum, columnsString(), isKey, unique, fksrc);
	}

	public String columnsString() {
		return Ints.join(",", columns);
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

	public boolean hasColumn(String name) {
		return ("," + columns + ",").contains("," + name + ",");
	}

	@Override
	public String toString() {
		return (isKey() ? "key" : "index") + (unique ? "unique" : "") +
				"(" + Ints.join(",", columns) + ")";
	}

	public String schema(StringBuilder sb, Columns cols) {
		if (isKey)
			sb.append(" key");
		else
			sb.append(" index").append(unique ? " unique" : "");
		sb.append("(").append(cols.names(columns)).append(")");
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
		return this.columnsString().compareTo(that.columnsString());
	}

}
