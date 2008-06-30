package suneido.database;

import static suneido.Suneido.verify;
import suneido.SuBoolean;
import suneido.SuString;
import suneido.SuValue;

/**
 * 
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small></p>
 */
public class Index {
	private final Record record;
	public final String columns;
	public final short[] colnums;
	public final BtreeIndex btreeIndex;
	final static int TBLNUM = 0, COLUMNS = 1, KEY = 2, FKTABLE = 3,
			FKCOLUMNS = 4, FKMODE = 5, ROOT = 6, TREELEVELS = 7, NNODES = 8;
	final static int BLOCK = 0, CASCADE_UPDATES = 1, CASCADE_DELETES = 2,
			CASCADE = 3;
	private final static SuString UNIQUE = new SuString("u");

	// Fkey fksrc;
	// ArrayList<Fkey> fkdsts;

	public Index(Record record, String columns, short[] colnums, BtreeIndex btreeIndex) {
		this.record = record;
		verify(record.off() != 0);
		this.columns = columns;
		this.colnums = colnums;
		this.btreeIndex = btreeIndex;
	}

	public void update() {
		verify(record.off() != 0);
		// treelevels and root should not change without nnodes changing
		if (record.getInt(NNODES) != btreeIndex.nnodes())
			indexInfo(record, btreeIndex);
	}

	public static Record record(BtreeIndex btreeIndex) {
		Record r = new Record()
				.add(btreeIndex.tblnum)
				.add(btreeIndex.index)
				.add(btreeIndex.iskey ? SuBoolean.TRUE :
						btreeIndex.unique ? UNIQUE : SuBoolean.FALSE)
				.add("") // fktable
				.add("") // fkcolumns
				.add(BLOCK);
		indexInfo(r, btreeIndex);
		r.alloc(24); // 24 = 3 fields * max int packsize - min int packsize
		return r;
	}

	private static void indexInfo(Record r, BtreeIndex btreeIndex) {
		r.reuse(ROOT);
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
}
