package suneido.database;

import static suneido.SuException.verify;

import java.util.List;

import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableList;

/**
 * Used by {@link Database} and {@link Indexes} to handle a single index.
 *
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public class Index {

	public static final int TBLNUM = 0, COLUMNS = 1, KEY = 2, FKTABLE = 3,
			FKCOLUMNS = 4, FKMODE = 5, ROOT = 6, TREELEVELS = 7, NNODES = 8;
	public static final int BLOCK = 0, CASCADE_UPDATES = 1,
			CASCADE_DELETES = 2, CASCADE = 3;
	static final String UNIQUE = "u";

	public final int tblnum;
	public final String columns;
	public final ImmutableList<Integer> colnums;
	public final boolean isKey;
	public final boolean unique;
	final ForeignKey fksrc;
	final ImmutableList<ForeignKey> fkdsts;

	public Index(Record record, String columns, ImmutableList<Integer> colnums,
			List<Record> fkdstrecs) {
		verify(record.off() != 0);
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

	@Override
	public String toString() {
		return "Index(" + columns + ")" + (isKey() ? ".key" : "")
				+ (unique ? ".unique" : "");
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

	public boolean isLower() {
		return false; // TODO isLower
	}

	@Immutable
	static class ForeignKey {
		final String tablename; // used by fksrc
		final int tblnum; // used by fkdsts
		final String columns;
		final int mode;

		static final ForeignKey NIL = new ForeignKey("", "", 0);

		ForeignKey(String tablename, String columns, int mode) {
			this(tablename, columns, mode, 0);
		}

		ForeignKey(int tblnum, String columns, int mode) {
			this(null, columns, mode, tblnum);
		}

		private ForeignKey(String tablename, String columns, int mode,
				int tblnum) {
			this.mode = mode;
			this.columns = columns.startsWith("lower:") ? columns.substring(6)
					: columns;
			this.tablename = tablename;
			this.tblnum = tblnum;
		}

		@Override
		public String toString() {
			return "ForeignKey(" + (tablename == null ? tblnum : tablename)
			+ ", " + columns + ", " + mode + ")";
		}
	}

	public boolean hasColumn(String name) {
		return ("," + columns + ",").contains("," + name + ",");
	}

}
