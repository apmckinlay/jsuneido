/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

import static suneido.SuException.verify;

import java.util.List;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableList;

/**
 * Used by {@link Database} and {@link Indexes} to handle a single index.
 */
class Index {
	static final int TBLNUM = 0, COLUMNS = 1, KEY = 2, FKTABLE = 3,
			FKCOLUMNS = 4, FKMODE = 5, ROOT = 6, TREELEVELS = 7, NNODES = 8;
	static final String UNIQUE = "u";
	final int tblnum;
	final String columns;
	final ImmutableList<Integer> colnums;
	final boolean isKey;
	final boolean unique;
	final ForeignKey fksrc;
	final ImmutableList<ForeignKey> fkdsts;

	Index(Record record, String columns, ImmutableList<Integer> colnums,
			List<Record> fkdstrecs) {
		verify(record.offset() != 0);
		this.tblnum = record.getInt(TBLNUM);
		this.columns = columns;
		this.colnums = colnums;
		Object key = record.get(KEY);
		this.isKey = key == Boolean.TRUE;
		this.unique = key.equals(UNIQUE);
		fksrc = get_fksrc(record);
		fkdsts = get_fkdsts(fkdstrecs);
	}

	private ForeignKey get_fksrc(Record record) {
		String fktable = record.getString(FKTABLE);
		if (!fktable.equals(""))
			return new ForeignKey(fktable, record.getString(FKCOLUMNS),
					record.getInt(FKMODE));
		return null;
	}

	private ImmutableList<ForeignKey> get_fkdsts(List<Record> fkdstrecs) {
		ImmutableList.Builder<ForeignKey> builder = ImmutableList.builder();
		for (Record ri : fkdstrecs)
			builder.add(new ForeignKey(ri.getInt(TBLNUM),
					ri.getString(COLUMNS),
					ri.getInt(FKMODE)));
		return builder.build();
	}

	static String getColumns(Record r) {
		return r.getString(COLUMNS);
	}

	boolean isKey() {
		return isKey;
	}

	@Immutable
	static class ForeignKey {
		final String tablename; // used by fksrc
		final int tblnum; // used by fkdsts
		final String columns;
		final int mode;

		ForeignKey(String tablename, String columns, int mode) {
			this(tablename, columns, mode, 0);
		}

		ForeignKey(int tblnum, String columns, int mode) {
			this(null, columns, mode, tblnum);
		}

		private ForeignKey(String tablename, String columns, int mode, int tblnum) {
			this.mode = mode;
			this.columns = columns;
			this.tablename = tablename;
			this.tblnum = tblnum;
		}

		@Override
		public String toString() {
			return "ForeignKey(" + (tablename == null ? tblnum : tablename)
			+ ", " + columns + ", " + mode + ")";
		}
	}

	boolean hasColumn(String name) {
		return ("," + columns + ",").contains("," + name + ",");
	}

	@Override
	public String toString() {
		return (isKey() ? "key" : "index") + (unique ? "unique" : "") +
				"(" + columns + ")";
	}

}
