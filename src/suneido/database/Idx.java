package suneido.database;

import static suneido.Suneido.verify;
import suneido.SuBoolean;
import suneido.SuString;
import suneido.SuValue;

public class Idx {
	public BtreeIndex btreeIndex;
	Record rec;
	String columns;
	short[] colnums;
	final static int TBLNUM = 0, COLUMNS = 1, KEY = 2, FKTABLE = 3,
			FKCOLUMNS = 4, FKMODE = 5, ROOT = 6, TREELEVELS = 7, NNODES = 8;
	final static int BLOCK = 0, CASCADE_UPDATES = 1, CASCADE_DELETES = 2,
			CASCADE = 3;
	private final static SuString UNIQUE = new SuString("u");

	// Fkey fksrc;
	// ArrayList<Fkey> fkdsts;

	public Idx(String table, Record r, String columns, short[] colnums,
			BtreeIndex btreeIndex) {
		this.btreeIndex = btreeIndex;
		this.rec = r;
		this.columns = columns;
		this.colnums = colnums;
	}

	public void update() {
		// TODO Auto-generated method stub

	}

	public static Record record(BtreeIndex btreeIndex) {
		Record r = new Record()
				.add(btreeIndex.tblnum)
				.add(btreeIndex.index)
				.add(btreeIndex.iskey ? SuBoolean.TRUE : SuBoolean.FALSE)
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
