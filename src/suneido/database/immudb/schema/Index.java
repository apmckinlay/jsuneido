/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb.schema;


import suneido.database.immudb.*;

import com.google.common.collect.ImmutableList;

public class Index {
	public static final int TBLNUM = 0, COLUMNS = 1, KEY = 2,
			FKTABLE = 3, FKCOLUMNS = 4, FKMODE = 5,
			ROOT = 6, TREELEVELS = 7, NNODES = 8;
	public static final int BLOCK = 0, CASCADE_UPDATES = 1,
			CASCADE_DELETES = 2, CASCADE = 3;
	static final String UNIQUE = "u";
	public final int tblnum;
	public final String columns;
	public final ImmutableList<Integer> colnums;
	public final boolean isKey;
	public final boolean unique;
	public final ForeignKey fksrc;
	public final BtreeInfo btreeInfo;


	/** key is false, true, or "u" */
	public Index(Columns cols, int tblnum, String columns, Object key,
			BtreeInfo btreeInfo) {
		this.tblnum = tblnum;
		this.columns = columns;
		this.isKey = key == Boolean.TRUE;
		this.unique = key.equals(UNIQUE);
		fksrc = null;
		this.btreeInfo = btreeInfo;
		colnums = cols.nums(columns);
	}

	public Index(Columns cols, Record record) {
		this.tblnum = record.getInt(TBLNUM);
		this.columns = record.getString(COLUMNS);
		Object key = record.get(KEY);
		this.isKey = key == Boolean.TRUE;
		this.unique = key.equals(UNIQUE);
		fksrc = get_fksrc(record);
		btreeInfo = btreeInfo(record);
		colnums = cols.nums(columns);
	}

	public static BtreeInfo btreeInfo(Record record) {
		return new BtreeInfo(record.getInt(ROOT),
				record.getInt(TREELEVELS), record.getInt(NNODES));
	}

	private ForeignKey get_fksrc(Record record) {
		String fktable = record.getString(FKTABLE);
		if (!fktable.equals(""))
			return new ForeignKey(fktable, record.getString(FKCOLUMNS),
					record.getInt(FKMODE));
		return null;
	}

	public Record toRecord() {
		return toRecord(tblnum, columns, isKey, unique, fksrc, btreeInfo);
	}

	public static Record toRecord(int tblnum, String columns, boolean isKey,
			boolean unique, ForeignKey fksrc, BtreeInfo btreeInfo) {
		RecordBuilder rb = new RecordBuilder();
		rb.add(tblnum).add(columns);
		if (unique)
			rb.add(UNIQUE);
		else
			rb.add(isKey);
		rb.add(fksrc.tablename).add(fksrc.columns).add(fksrc.mode);
		rb.add(btreeInfo.root).add(btreeInfo.treeLevels).add(btreeInfo.nnodes);
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

	@Override
	public String toString() {
		return (isKey() ? "key" : "index") + (unique ? "unique" : "") +
				"(" + columns + ")";
	}

	public static String getColumns(Record r) {
		String columns = r.getString(COLUMNS);
		if (columns.startsWith("lower:"))
			columns = columns.substring(6);
		return columns;
	}

	public boolean isKey() {
		return isKey;
	}

	public boolean hasColumn(String name) {
		return ("," + columns + ",").contains("," + name + ",");
	}



}
