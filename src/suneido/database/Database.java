package suneido.database;

import static java.lang.Math.min;
import static suneido.Suneido.verify;

import java.nio.ByteBuffer;
import java.util.zip.Adler32;

import suneido.SuBoolean;
import suneido.SuException;
import suneido.SuString;
import suneido.SuValue;

/**
 * Implements the Suneido database. Uses {@link Mmfile} and {@link BtreeIndex}.
 * 
 * @author Andrew McKinlay
 *         <p>
 *         <small>Copyright 2008 Suneido Software Corp. All rights reserved.
 *         Licensed under GPLv2.</small>
 *         </p>
 */
public class Database implements Destination {
	private final Mmfile mmf;
	private Dbhdr dbhdr;
	private boolean loading = false;
	// private long clock = 1;
	private final Adler32 cksum = new Adler32();
	private byte output_type = Mmfile.DATA;
	private final Tables tables = new Tables();

	private static class TN {
		final static int TABLES = 0, COLUMNS = 1, INDEXES = 2, VIEWS = 3;
	}

	private static class FKMODE {
		final static int BLOCK = 0, CASCADE_UPDATES = 1, CASCADE_DELETES = 2,
				CASCADE = 3;
	}

	private static class I {
		final static int TBLNUM = 0, COLUMNS = 1, KEY = 2, FKTABLE = 3,
				FKCOLUMNS = 4, FKMODE = 5, ROOT = 6, TREELEVELS = 7,
				NNODES = 8;
	}

	private static class T {
		final static int TBLNUM = 0, TABLE = 1, NEXTFIELD = 2, NROWS = 3,
				TOTALSIZE = 4;
	}

	private static class C {
		final static int TBLNUM = 0, COLUMN = 1, FLDNUM = 2;
	}

	private static class V {
		final static int NAME = 0, DEFINITION = 1;
	}

	public final static int SCHEMA_TRAN = 0;
	private final static int VERSION = 1;
	private BtreeIndex tables_index;
	private BtreeIndex tblnum_index;
	private BtreeIndex columns_index;
	private BtreeIndex indexes_index;
	private BtreeIndex fkey_index;
	// used by get_view
	private BtreeIndex views_index;

	public Database(String filename, Mode mode) {
		mmf = new Mmfile(filename, mode);
		// if (mode == Mode.OPEN && ! check_shutdown(mmf)) {
		// mmf.close();
		// if (0 != fork_rebuild())
		// fatal("Database not rebuilt, unable to start");
		// mmf = new Mmfile(filename, mode);
		// verify(check_shutdown(mmf));
		// }
		if (mode == Mode.CREATE) {
			output_type = Mmfile.OTHER;
			create();
			output_type = Mmfile.DATA;
		} else {
			open();
		}
	}

	private void create() {
		loading = true;

		long dbhdr_at = alloc(Dbhdr.SIZE, Mmfile.OTHER);

		// tables
		tables_index = new BtreeIndex(this, TN.TABLES, "tablename", true, false);
		tblnum_index = new BtreeIndex(this, TN.TABLES, "table", true, false);
		table_record(TN.TABLES, "tables", 3, 5);
		table_record(TN.COLUMNS, "columns", 17, 3);
		table_record(TN.INDEXES, "indexes", 5, 9);

		// columns
		columns_index = new BtreeIndex(this, TN.COLUMNS, "table,column", true,
				false);
		columns_record(TN.TABLES, "table", 0);
		columns_record(TN.TABLES, "tablename", 1);
		columns_record(TN.TABLES, "nextfield", 2);
		columns_record(TN.TABLES, "nrows", 3);
		columns_record(TN.TABLES, "totalsize", 4);

		columns_record(TN.COLUMNS, "table", 0);
		columns_record(TN.COLUMNS, "column", 1);
		columns_record(TN.COLUMNS, "field", 2);

		columns_record(TN.INDEXES, "table", 0);
		columns_record(TN.INDEXES, "columns", 1);
		columns_record(TN.INDEXES, "key", 2);
		columns_record(TN.INDEXES, "fktable", 3);
		columns_record(TN.INDEXES, "fkcolumns", 4);
		columns_record(TN.INDEXES, "fkmode", 5);
		columns_record(TN.INDEXES, "root", 6);
		columns_record(TN.INDEXES, "treelevels", 7);
		columns_record(TN.INDEXES, "nnodes", 8);

		// indexes
		indexes_index = new BtreeIndex(this, TN.INDEXES, "table,columns", true,
				false);
		fkey_index = new BtreeIndex(this, TN.INDEXES, "fktable,fkcolumns",
				false, false);
		indexes_record(tables_index);
		indexes_record(tblnum_index);
		indexes_record(columns_index);
		// output indexes indexes last
		long indexes_adr = indexes_record(indexes_index);
		indexes_record(fkey_index);

		// views
		// add_table("views");
		// add_column("views", "view_name");
		// add_column("views", "view_definition");
		// add_index("views", "view_name", true);

		dbhdr = new Dbhdr(dbhdr_at, indexes_adr);
	}

	private void table_record(int tblnum, String tblname, int nrows,
			int nextfield) {
		Record r = new Record();
		r.add(tblnum).add(tblname).add(nextfield).add(nrows).add(100);
		r.alloc(24); // 24 = 3 fields * max int packsize - min int packsize
		long at = output(TN.TABLES, r);
		verify(tblnum_index.insert(SCHEMA_TRAN, new Slot(new Record().add(
				tblnum).addMmoffset(at))));
		verify(tables_index.insert(SCHEMA_TRAN, new Slot(new Record().add(
				tblname).addMmoffset(at))));
	}

	private void columns_record(int tblnum, String column, int field) {
		Record r = new Record();
		r.add(tblnum);
		r.add(column);
		r.add(field);
		long at = output(TN.COLUMNS, r);
		Record key = new Record().add(tblnum).add(column).addMmoffset(at);
		verify(columns_index.insert(SCHEMA_TRAN, new Slot(key)));
	}

	private long indexes_record(BtreeIndex btreeIndex) {
		Record r = new Record().add(btreeIndex.tblnum).add(btreeIndex.index)
				.add(btreeIndex.iskey ? SuBoolean.TRUE : SuBoolean.FALSE).add(
						"") // fktable
				.add("") // fkcolumns
				.add(FKMODE.BLOCK);
		indexInfo(r, btreeIndex);
		r.alloc(24); // 24 = 3 fields * max int packsize - min int packsize
		long at = output(TN.INDEXES, r);
		Record key1 = new Record().add(btreeIndex.tblnum).add(btreeIndex.index)
				.addMmoffset(at);
		verify(indexes_index.insert(SCHEMA_TRAN, new Slot(key1)));
		Record key2 = new Record().add("").add("").addMmoffset(at);
		verify(fkey_index.insert(SCHEMA_TRAN, new Slot(key2)));
		return at;
	}

	private void indexInfo(Record r, BtreeIndex btreeIndex) {
		r.reuse(I.ROOT);
		r.addMmoffset(btreeIndex.root());
		r.add(btreeIndex.treelevels());
		r.add(btreeIndex.nnodes());
	}

	void open() {
		dbhdr = new Dbhdr();
		Session.startup(mmf);
		mmf.sync();
		indexes_index = mkindex(input(dbhdr.indexes));

		Record r = find(SCHEMA_TRAN, indexes_index, key(TN.INDEXES,
				"table,columns"));
		verify(!r.isEmpty() && r.off() == dbhdr.indexes);

		tables_index = mkindex(find(SCHEMA_TRAN, indexes_index, key(TN.TABLES,
				"tablename")));
		tblnum_index = mkindex(find(SCHEMA_TRAN, indexes_index, key(TN.TABLES,
				"table")));
		columns_index = mkindex(find(SCHEMA_TRAN, indexes_index, key(
				TN.COLUMNS, "table,column")));
		fkey_index = mkindex(find(SCHEMA_TRAN, indexes_index, key(TN.INDEXES,
				"fktable,fkcolumns")));
		// WARNING: any new indexes added here must also be added in get_table
	}

	private final static SuString UNIQUE = new SuString("u");

	BtreeIndex mkindex(Record r) {
		String columns = r.getString(I.COLUMNS);
		// boolean lower = columns.startsWith("lower:");
		// if (lower)
		// columns += 6;
		SuValue key = r.get(I.KEY);
		long root = r.getMmoffset(I.ROOT);
		verify(root != 0);
		return new BtreeIndex(this, r.getInt(I.TBLNUM), columns,
				key == SuBoolean.TRUE, key.equals(UNIQUE), root, r
						.getInt(I.TREELEVELS), r.getInt(I.NNODES));
	}

	public Record input(long adr) {
		verify(adr != 0);
		return new Record(mmf.adr(adr), adr);
	}

	private Record key(int tblnum, String columns) {
		return new Record().add(tblnum).add(columns);
	}

	private Record key(int i) {
		return new Record().add(i);
	}

	private Record key(String s) {
		return new Record().add(s);
	}

	private Record find(int tran, BtreeIndex btreeIndex, Record key) {
		Slot slot = btreeIndex.find(tran, key);
		return slot.isEmpty() ? Record.MINREC : input(slot.keyadr());
	}

	public void close() {
		Session.shutdown(mmf);
		mmf.close();
	}

	Table ck_get_table(String table) {
		Table tbl = get_table(table);
		if (tbl == null)
			throw new SuException("nonexistent table: " + table);
		return tbl;
	}

	Table get_table(String table) {
		// if the table has already been read, return it
		Table tbl = tables.get(table);
		if (tbl != null) {
			verify(tbl.name == table);
			return tbl;
		}
		return get_table(find(SCHEMA_TRAN, tables_index, key(table)));
	}

	Table get_table(int tblnum) {
		// if the table has already been read, return it
		Table tbl = tables.get(tblnum);
		if (tbl != null) {
			verify(tbl.num == tblnum);
			return tbl;
		}
		return get_table(find(SCHEMA_TRAN, tblnum_index, key(tblnum)));
	}

	Table get_table(Record table_rec) {
		if (table_rec == null)
			return null; // table not found
		String table = table_rec.getString(T.TABLE);

		BtreeIndex.Iter iter;
		Record tblkey = key(table_rec.getInt(T.TBLNUM));

		// columns
		Columns columns = new Columns();
		for (iter = columns_index.iter(SCHEMA_TRAN, tblkey).next(); !iter.eof(); iter
				.next()) {
			Record r = iter.data();
			String column = r.getString(C.COLUMN);
			short colnum = r.getShort(C.FLDNUM);
			columns.add(new Column(column, colnum));
		}
		columns.sort();

		// have to do this before indexes since they may need it
		Table tbl = new Table(table_rec, columns, new Indexes());
		tables.add(tbl);

		// indexes
		Indexes idxs = new Indexes();
		// for (iter = indexes_index.begin(SCHEMA_TRAN, tblkey); !iter.eof();
		// ++iter) {
		// Record r = iter.data();
		// String columns = r.getString(I.COLUMNS);// .to_heap();
		// if (columns.startsWith("lower:"))
		// columns = columns.substring(6);
		// short[] colnums = comma_to_nums(cols, columns);
		// verify(colnums != null);
		// // make sure to use the same index for the system tables
		// Index index;
		// if (table == "tables" && columns == "tablename")
		// index = tables_index;
		// else if (table == "tables" && columns == "table")
		// index = tblnum_index;
		// else if (table == "columns" && columns == "table,column")
		// index = columns_index;
		// else if (table == "indexes" && columns == "table,columns")
		// index = indexes_index;
		// else if (table == "indexes" && columns == "fktable,fkcolumns")
		// index = fkey_index;
		// else
		// index = mkindex(r);
		// idxs.add(new Idx(table, r, columns, colnums, index));
		// }
		tbl.indexes = idxs;
		return tbl;
	}

	void add_any_record(int tran, Table tbl, Record r) {
		// if (tran != SCHEMA_TRAN && ck_get_tran(tran).type != READWRITE)
		// throw new SuException("can't output from read-only transaction to " +
		// tbl.name);
		verify(tbl != null);
		verify(!tbl.indexes.isEmpty());

		// if (! loading)
		// if (String cols = fkey_source_block(tran, tbl, r))
		// throw new SuException("add record: blocked by foreign key: " + cols +
		// " in " + tbl.name);

		if (tbl.num > TN.VIEWS && r.size() > tbl.nextfield)
			throw new SuException("output: record has more fields (" + r.size()
					+ ") than " + tbl.name + " should (" + tbl.nextfield + ")");
		long off = output(tbl.num, r);
		try {
			add_index_entries(tran, tbl, r);
		} finally {
			if (tran == SCHEMA_TRAN)
				delete_act(tran, tbl.num, off);
		}
		create_act(tran, tbl.num, off);

		if (!loading)
			tbl.user_trigger(tran, Record.MINREC, r);
	}

	void add_index_entries(int tran, Table tbl, Record r) {
		long off = r.off();
		for (Index i : tbl.indexes) {
			Record key = r.project(i.colnums, off);
			// handle insert failing due to duplicate key
			if (!i.btreeIndex.insert(tran, new Slot(key))) {
				// delete from previous indexes
				for (Index j : tbl.indexes) {
					if (j == i)
						break;
					key = r.project(j.colnums, off);
					verify(j.btreeIndex.erase(key));
				}
				throw new SuException("duplicate key: " + i.columns + " = "
						+ key + " in " + tbl.name);
			}
			i.update(); // update indexes record from index
		}

		++tbl.nrecords;
		tbl.totalsize += r.bufSize();
		tbl.update(); // update tables record
	}

	long output(int tblnum, Record r) {
		int n = r.packSize();
		long offset = alloc(4 + n, output_type);
		ByteBuffer p = adr(offset);
		p.putInt(tblnum);
		r.pack(p);
		// don't checksum tables or indexes records because they get updated
		if (output_type == Mmfile.DATA && tblnum != TN.TABLES
				&& tblnum != TN.INDEXES)
			checksum(p, 4 + n);
		return offset + 4; // offset of record i.e. past tblnum
	}

	public long alloc(int n) {
		return alloc(n, Mmfile.OTHER);
	}

	long alloc(int n, byte type) {
		return mmf.alloc(n, type);
	}

	public ByteBuffer adr(long offset) {
		return mmf.adr(offset);
	}

	public long size() {
		return mmf.size();
	}

	static byte[] bytes = new byte[256];

	void checksum(ByteBuffer buf, int len) {
		for (int i = 0; i < len; i += bytes.length) {
			int n = min(bytes.length, len - i);
			buf.get(bytes, i, n);
			cksum.update(bytes, 0, n);
		}
	}

	private class Dbhdr {
		static final int SIZE = 4 + 4 + 4;
		ByteBuffer buf;
		int next_table;
		long indexes;

		// create
		Dbhdr(long at, long indexes_adr) {
			verify(at == mmf.first());
			buf = adr(at);
			buf.putInt(next_table = TN.INDEXES + 1);
			buf.putInt(Mmfile.offsetToInt(indexes = indexes_adr));
			buf.putInt(VERSION);
		}

		// open
		Dbhdr() {
			long at = mmf.first();
			if (mmf.length(at) < SIZE)
				throw new SuException("invalid database");
			buf = adr(mmf.first());
			next_table = buf.getInt();
			indexes = Mmfile.intToOffset(buf.getInt());
			int version = buf.getInt();
			if (version != VERSION)
				throw new SuException("invalid database");
		}

	}

	public TranRead read_act(int tran, int tblnum, String index) {
		// TODO Auto-generated method stub
		return new TranRead(tblnum, index);
	}

	private void delete_act(int tran, int num, long off) {
		// TODO Auto-generated method stub

	}

	private void create_act(int tran, int num, long off) {
		// TODO Auto-generated method stub

	}

	public boolean visible(int tran, long adr) {
		// TODO Auto-generated method stub
		return true;
	}
}
