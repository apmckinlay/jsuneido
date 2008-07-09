package suneido.database;

import static suneido.Suneido.verify;

import java.util.ArrayList;
import java.util.List;

import suneido.SuBoolean;
import suneido.SuString;
import suneido.SuValue;

/**
 * Used by {@link Database} and {@link Indexes} to handle a single index.
 *
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public class Index {
	private final Record record;
	public final String columns;
	public final short[] colnums;
	public final BtreeIndex btreeIndex;
	final static int TBLNUM = 0, COLUMNS = 1, KEY = 2, FKTABLE = 3,
		FKCOLUMNS = 4, FKMODE = 5, ROOT = 6, TREELEVELS = 7, NNODES = 8;
	public final static int BLOCK = 0, CASCADE_UPDATES = 1,
		CASCADE_DELETES = 2, CASCADE = 3;
	private final static SuString UNIQUE = new SuString("u");

	ForeignKey fksrc = null;
	ArrayList<ForeignKey> fkdsts = new ArrayList<ForeignKey>();

	public Index(Record record, String columns, short[] colnums,
			BtreeIndex btreeIndex, List<Record> fkdstrecs) {
		this.record = record;
		verify(record.off() != 0);
		this.columns = columns;
		this.colnums = colnums;
		this.btreeIndex = btreeIndex;

		fksrc = new ForeignKey(record.getString(FKTABLE),
				record.getString(FKCOLUMNS), record.getInt(FKMODE));

		for (Record ri : fkdstrecs)
			fkdsts.add(new ForeignKey(ri.getInt(TBLNUM), ri.getString(COLUMNS),
					ri.getInt(FKMODE)));
	}

	@Override
	public String toString() {
		return "Index(" + columns + ")" + (iskey() ? ", key" : "")
				+ (btreeIndex.unique ? "unique" : "");
	}

	public void update() {
		verify(record.off() != 0);
		// treelevels and root should not change without nnodes changing
		if (record.getInt(NNODES) != btreeIndex.nnodes())
			indexInfo(record, btreeIndex);
	}
	
	public Record record() {
		ForeignKey fk = fksrc == null ? ForeignKey.NIL : fksrc;
		return Index.record(btreeIndex, fk.tablename, fk.columns, fk.mode);
	}
	
	public static Record record(BtreeIndex btreeIndex) {
		return record(btreeIndex, null, null, 0);
	}
	public static Record record(BtreeIndex btreeIndex, 
			String fktable, String fkcolumns, int fkmode) {
		Record r = new Record()
			.add(btreeIndex.tblnum)
			.add(btreeIndex.indexColumns)
			.add(btreeIndex.iskey ? SuBoolean.TRUE :
				btreeIndex.unique ? UNIQUE : SuBoolean.FALSE)
			.add(fktable).add(fkcolumns).add(fkmode);
		indexInfo(r, btreeIndex);
		r.alloc(24); // 24 = 3 fields * max int packsize - min int packsize
		return r;
	}

	private static void indexInfo(Record r, BtreeIndex btreeIndex) {
		r.truncate(ROOT);
		r.addMmoffset(btreeIndex.root());
		r.add(btreeIndex.treelevels());
		r.add(btreeIndex.nnodes());
	}

	public static BtreeIndex btreeIndex(Destination dest, Record r) {
		String columns = r.getString(COLUMNS);
		// boolean lower = columns.startsWith("lower:");
		// if (lower)
		// columns += 6;
		SuValue key = r.get(KEY);
		long root = r.getMmoffset(ROOT);
		verify(root != 0);
		return new BtreeIndex(dest, r.getInt(TBLNUM), columns,
				key == SuBoolean.TRUE, key.equals(UNIQUE), root,
				r.getInt(TREELEVELS), r.getInt(NNODES));
	}

	public static String getColumns(Record r) {
		String columns = r.getString(COLUMNS);
		if (columns.startsWith("lower:"))
			columns = columns.substring(6);
		return columns;
	}

	public boolean iskey() {
		return btreeIndex.iskey;
	}

	static class ForeignKey {
		String tablename; // used by fksrc
		int tblnum; // used by fkdsts
		String columns;
		int mode;
		
		static final ForeignKey NIL = new ForeignKey("", "", 0);

		ForeignKey(String tablename, String columns, int mode) {
			this(columns, mode);
			this.tablename = tablename;
		}

		ForeignKey(int tblnum, String columns, int mode) {
			this(columns, mode);
			this.tblnum = tblnum;
		}

		private ForeignKey(String columns, int mode) {
			this.mode = mode;
			this.columns = columns.startsWith("lower:") ? columns.substring(6)
					: columns;
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
