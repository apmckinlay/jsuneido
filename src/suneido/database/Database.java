package suneido.database;

import static java.lang.Math.min;
import static suneido.Suneido.verify;

import java.nio.ByteBuffer;
import java.util.zip.Adler32;

import suneido.SuException;

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
	private long clock = 1;
	private final Adler32 cksum = new Adler32();
	private byte output_type = Mmfile.DATA;
	private final Tables tables = new Tables();

	private static class TN {
		final static int TABLES = 0, COLUMNS = 1, INDEXES = 2, VIEWS = 3;
	}

	private static class V {
		final static int NAME = 0, DEFINITION = 1;
	}

	public final static int SCHEMA_TRAN = 0;
	private final static int VERSION = 1;
	private BtreeIndex tablename_index;
	private BtreeIndex tablenum_index;
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
		tablename_index = new BtreeIndex(this, TN.TABLES, "tablename", true, false);
		tablenum_index = new BtreeIndex(this, TN.TABLES, "table", true, false);
		createTable("tables", TN.TABLES, 5, 3);
		createTable("columns", TN.COLUMNS, 3, 17);
		createTable("indexes", TN.INDEXES, 9, 5);

		// columns
		columns_index = new BtreeIndex(this, TN.COLUMNS, "table,column", true, false);
		createColumn(TN.TABLES, "table", 0);
		createColumn(TN.TABLES, "tablename", 1);
		createColumn(TN.TABLES, "nextfield", 2);
		createColumn(TN.TABLES, "nrows", 3);
		createColumn(TN.TABLES, "totalsize", 4);

		createColumn(TN.COLUMNS, "table", 0);
		createColumn(TN.COLUMNS, "column", 1);
		createColumn(TN.COLUMNS, "field", 2);

		createColumn(TN.INDEXES, "table", 0);
		createColumn(TN.INDEXES, "columns", 1);
		createColumn(TN.INDEXES, "key", 2);
		createColumn(TN.INDEXES, "fktable", 3);
		createColumn(TN.INDEXES, "fkcolumns", 4);
		createColumn(TN.INDEXES, "fkmode", 5);
		createColumn(TN.INDEXES, "root", 6);
		createColumn(TN.INDEXES, "treelevels", 7);
		createColumn(TN.INDEXES, "nnodes", 8);

		// indexes
		indexes_index = new BtreeIndex(this, TN.INDEXES, "table,columns", true, false);
		fkey_index = new BtreeIndex(this, TN.INDEXES, "fktable,fkcolumns", false, false);
		createIndex(tablename_index);
		createIndex(tablenum_index);
		createIndex(columns_index);
		// output indexes indexes last
		long indexes_adr = createIndex(indexes_index);
		createIndex(fkey_index);

		// views
		// add_table("views");
		// add_column("views", "view_name");
		// add_column("views", "view_definition");
		// add_index("views", "view_name", true);

		dbhdr = new Dbhdr(dbhdr_at, indexes_adr);
	}

	private void createTable(String name, int num, int nextfield, int nrecords) {
		long at = output(TN.TABLES,
				Table.record(name, num, nextfield, nrecords));
		verify(tablenum_index.insert(SCHEMA_TRAN,
				new Slot(new Record().add(num).addMmoffset(at))));
		verify(tablename_index.insert(SCHEMA_TRAN,
				new Slot(new Record().add(name).addMmoffset(at))));
	}

	private void createColumn(int tblnum, String column, int field) {
		long at = output(TN.COLUMNS, Column.record(tblnum, column, field));
		Record key = new Record().add(tblnum).add(column).addMmoffset(at);
		verify(columns_index.insert(SCHEMA_TRAN, new Slot(key)));
	}

	private long createIndex(BtreeIndex btreeIndex) {
		long at = output(TN.INDEXES, Idx.record(btreeIndex));
		Record key1 = new Record().add(btreeIndex.tblnum).add(btreeIndex.index)
				.addMmoffset(at);
		verify(indexes_index.insert(SCHEMA_TRAN, new Slot(key1)));
		Record key2 = new Record().add("").add("").addMmoffset(at);
		verify(fkey_index.insert(SCHEMA_TRAN, new Slot(key2)));
		return at;
	}

	void open() {
		dbhdr = new Dbhdr();
		Session.startup(mmf);
		mmf.sync();
		indexes_index = Idx.btreeIndex(this, input(dbhdr.indexes));

		Record r = find(SCHEMA_TRAN, indexes_index,
				key(TN.INDEXES, "table,columns"));
		verify(!r.isEmpty() && r.off() == dbhdr.indexes);

		tablename_index = btreeIndex(TN.TABLES, "tablename");
		tablenum_index = btreeIndex(TN.TABLES, "table");
		columns_index = btreeIndex(TN.COLUMNS, "table,column");
		fkey_index = btreeIndex(TN.INDEXES, "fktable,fkcolumns");
		// WARNING: any new indexes added here must also be added in get_table
	}

	private BtreeIndex btreeIndex(int table_num, String columns) {
		return Idx.btreeIndex(this, find(SCHEMA_TRAN, indexes_index, key(
				table_num, columns)));
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
		return slot.isEmpty() ? null : input(slot.keyadr());
	}

	public void close() {
		Session.shutdown(mmf);
		mmf.close();
	}

	void addTable(String table) {
		if (tableExists(table))
			throw new SuException("add table: table already exists: " + table);
		int tblnum = dbhdr.next_table++;
		Record r = Table.record(table, tblnum, 0, 0);
		add_any_record(SCHEMA_TRAN, "tables", r);
		table_create_act(tblnum);
	}

	void addColumn(String table, String column) {
		Table tbl = ck_getTable(table);

		int fldnum = Character.isUpperCase(column.charAt(0)) ? -1 : tbl.nextfield;
		if (! column.equals("-")) { // addition of deleted field used by dump/load
			column = column.toLowerCase();
			if (tbl.hasColumn(column))
				throw new SuException("add column: column already exists: " + column + " in " + table);
			Record rec = Column.record(tbl.num, column, fldnum);
			add_any_record(SCHEMA_TRAN, "columns", rec);
			}
		if (fldnum >= 0) {
			++tbl.nextfield;
			tbl.update();
			}
		tables.remove(table);
	}

	void addIndex(String table, String columns, boolean isKey, String fktable,
			String fkcolumns, int fkmode, boolean unique, boolean lower) {
		Table tbl = ck_getTable(table);
		short[] colnums = tbl.columns.nums(columns);
		if (tbl.hasIndex(columns))
			throw new SuException("add index: index already exists: " + columns + " in " + table);
		BtreeIndex index = new BtreeIndex(this, tbl.num, columns, isKey, unique);

		if (tbl.hasIndexes() && tbl.hasRecords())
			{
			// insert existing records
			Idx idx = tbl.firstIndex();
			Table fktbl = getTable(fktable);
			for (BtreeIndex.Iter iter = idx.btreeIndex.iter(SCHEMA_TRAN).next(); ! iter.eof(); iter.next())
				{
				Record r = iter.data();
				// if (fkey_source_block(SCHEMA_TRAN, fktbl, fkcolumns,
				// r.project(colnums)))
				// throw new SuException("add index: blocked by foreign key: " +
				// columns + " in " + table);
				Record key = r.project(colnums, iter.cur().keyadr());
				if (! index.insert(SCHEMA_TRAN, new Slot(key)))
					throw new SuException("add index: duplicate key: " + columns + " = " + key + " in " + table);
				}
			}

		Record r = Idx.record(index);
		add_any_record(SCHEMA_TRAN, "indexes", r);
		tbl.addIndex(new Idx(table, r, columns, colnums, index));

		if (fktable != "")
			tables.remove(fktable); // update target
	}

	Table ck_getTable(String table) {
		Table tbl = getTable(table);
		if (tbl == null)
			throw new SuException("nonexistent table: " + table);
		return tbl;
	}

	boolean tableExists(String table) {
		return getTable(table) != null;
	}

	Table getTable(String table) {
		// if the table has already been read, return it
		Table tbl = tables.get(table);
		if (tbl != null) {
			verify(tbl.name == table);
			return tbl;
		}
		return getTable(find(SCHEMA_TRAN, tablename_index, key(table)));
	}

	Table getTable(int tblnum) {
		// if the table has already been read, return it
		Table tbl = tables.get(tblnum);
		if (tbl != null) {
			verify(tbl.num == tblnum);
			return tbl;
		}
		return getTable(find(SCHEMA_TRAN, tablenum_index, key(tblnum)));
	}

	Table getTable(Record table_rec) {
		if (table_rec == null)
			return null; // table not found

		Table table = new Table(table_rec);

		Record tblkey = key(table.num);

		// columns
		for (BtreeIndex.Iter iter = columns_index.iter(SCHEMA_TRAN, tblkey)
				.next();
				!iter.eof(); iter.next())
			table.addColumn(new Column(iter.data()));
		table.sortColumns();

		// have to do this before indexes since they may need it

		// indexes
		for (BtreeIndex.Iter iter = indexes_index.iter(SCHEMA_TRAN, tblkey)
				.next(); !iter.eof(); iter
				.next()) {
			Record r = iter.data();
			String cols = Idx.getColumns(r);
			// make sure to use the same index for the system tables
			BtreeIndex index;
			if (table.name.equals("tables") && cols == "tablename")
				index = tablename_index;
			else if (table.name.equals("tables") && cols.equals("table"))
				index = tablenum_index;
			else if (table.name.equals("columns") && cols.equals("table,column"))
				index = columns_index;
			else if (table.name.equals("indexes") && cols.equals("table,columns"))
				index = indexes_index;
			else if (table.name.equals("indexes") && cols.equals("fktable,fkcolumns"))
				index = fkey_index;
			else
				index = Idx.btreeIndex(this, r);
			table.addIndex(new Idx(table.name, r, cols, table.columns
					.nums(cols),
					index));
		}
		tables.add(table);
		return table;
	}

	void add_any_record(int tran, String table, Record r) {
		add_any_record(tran, ck_getTable(table), r);
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
		long adr = output(tbl.num, r);
		try {
			add_index_entries(tran, tbl, r, adr);
		} finally {
			if (tran == SCHEMA_TRAN)
				delete_act(tran, tbl.num, adr);
		}
		create_act(tran, tbl.num, adr);

		if (!loading)
			tbl.user_trigger(tran, Record.MINREC, r);
	}

	void add_index_entries(int tran, Table tbl, Record r, long adr) {
		for (Idx i : tbl.indexes) {
			Record key = r.project(i.colnums, adr);
			// handle insert failing due to duplicate key
			if (!i.btreeIndex.insert(tran, new Slot(key))) {
				// delete from previous indexes
				for (Idx j : tbl.indexes) {
					if (j == i)
						break;
					key = r.project(j.colnums, adr);
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
		if (output_type == Mmfile.DATA &&
				tblnum != TN.TABLES	&& tblnum != TN.INDEXES)
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

	void table_create_act(int tblnum) {
		++clock;
//		table_created[tblnum] = clock;
		++clock;
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
