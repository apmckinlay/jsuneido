package suneido.database;

import static java.lang.Math.min;
import static suneido.Suneido.verify;
import static suneido.database.Transaction.NULLTRAN;

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

//	private static class V {
//		final static int NAME = 0, DEFINITION = 1;
//	}

	private final static int VERSION = 1;
	private BtreeIndex tablename_index;
	private BtreeIndex tablenum_index;
	private BtreeIndex columns_index;
	private BtreeIndex indexes_index;
	private BtreeIndex fkey_index;
	// used by get_view
//	private BtreeIndex views_index;

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
		createSchemaTable("tables", TN.TABLES, 5, 3);
		createSchemaTable("columns", TN.COLUMNS, 3, 17);
		createSchemaTable("indexes", TN.INDEXES, 9, 5);

		// columns
		columns_index = new BtreeIndex(this, TN.COLUMNS, "table,column", true, false);
		createSchemaColumn(TN.TABLES, "table", 0);
		createSchemaColumn(TN.TABLES, "tablename", 1);
		createSchemaColumn(TN.TABLES, "nextfield", 2);
		createSchemaColumn(TN.TABLES, "nrows", 3);
		createSchemaColumn(TN.TABLES, "totalsize", 4);

		createSchemaColumn(TN.COLUMNS, "table", 0);
		createSchemaColumn(TN.COLUMNS, "column", 1);
		createSchemaColumn(TN.COLUMNS, "field", 2);

		createSchemaColumn(TN.INDEXES, "table", 0);
		createSchemaColumn(TN.INDEXES, "columns", 1);
		createSchemaColumn(TN.INDEXES, "key", 2);
		createSchemaColumn(TN.INDEXES, "fktable", 3);
		createSchemaColumn(TN.INDEXES, "fkcolumns", 4);
		createSchemaColumn(TN.INDEXES, "fkmode", 5);
		createSchemaColumn(TN.INDEXES, "root", 6);
		createSchemaColumn(TN.INDEXES, "treelevels", 7);
		createSchemaColumn(TN.INDEXES, "nnodes", 8);

		// indexes
		indexes_index = new BtreeIndex(this, TN.INDEXES, "table,columns", true, false);
		fkey_index = new BtreeIndex(this, TN.INDEXES, "fktable,fkcolumns", false, false);
		createSchemaIndex(tablename_index);
		createSchemaIndex(tablenum_index);
		createSchemaIndex(columns_index);
		// output indexes indexes last
		long indexes_adr = createSchemaIndex(indexes_index);
		createSchemaIndex(fkey_index);

		// views
		// add_table("views");
		// add_column("views", "view_name");
		// add_column("views", "view_definition");
		// add_index("views", "view_name", true);

		dbhdr = new Dbhdr(dbhdr_at, indexes_adr);
	}

	private void createSchemaTable(String name, int num, int nextfield, int nrecords) {
		long at = output(TN.TABLES,
				Table.record(name, num, nextfield, nrecords));
		verify(tablenum_index.insert(NULLTRAN,
				new Slot(new Record().add(num).addMmoffset(at))));
		verify(tablename_index.insert(NULLTRAN,
				new Slot(new Record().add(name).addMmoffset(at))));
	}

	private void createSchemaColumn(int tblnum, String column, int field) {
		long at = output(TN.COLUMNS, Column.record(tblnum, column, field));
		Record key = new Record().add(tblnum).add(column).addMmoffset(at);
		verify(columns_index.insert(NULLTRAN, new Slot(key)));
	}

	private long createSchemaIndex(BtreeIndex btreeIndex) {
		long at = output(TN.INDEXES, Index.record(btreeIndex));
		Record key1 = new Record().add(btreeIndex.tblnum).add(btreeIndex.index)
				.addMmoffset(at);
		verify(indexes_index.insert(NULLTRAN, new Slot(key1)));
		Record key2 = new Record().add("").add("").addMmoffset(at);
		verify(fkey_index.insert(NULLTRAN, new Slot(key2)));
		return at;
	}

	private void open() {
		dbhdr = new Dbhdr();
		Session.startup(mmf);
		mmf.sync();
		indexes_index = Index.btreeIndex(this, input(dbhdr.indexes));

		Record r = find(NULLTRAN, indexes_index,
				key(TN.INDEXES, "table,columns"));
		verify(!r.isEmpty() && r.off() == dbhdr.indexes);

		tablename_index = btreeIndex(TN.TABLES, "tablename");
		tablenum_index = btreeIndex(TN.TABLES, "table");
		columns_index = btreeIndex(TN.COLUMNS, "table,column");
		fkey_index = btreeIndex(TN.INDEXES, "fktable,fkcolumns");
		// WARNING: any new indexes added here must also be added in get_table
	}

	private BtreeIndex btreeIndex(int table_num, String columns) {
		return Index.btreeIndex(this, find(NULLTRAN, indexes_index, key(
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

	private Record find(Transaction tran, BtreeIndex btreeIndex, Record key) {
		Slot slot = btreeIndex.find(tran, key);
		return slot.isEmpty() ? null : input(slot.keyadr());
	}

	public void close() {
		Session.shutdown(mmf);
		mmf.close();
	}

	public void addTable(Transaction tran, String table) {
		if (tableExists(table))
			throw new SuException("add table: table already exists: " + table);
		int tblnum = dbhdr.next_table++;
		Record r = Table.record(table, tblnum, 0, 0);
		add_any_record(tran, "tables", r);
	}

	public void addColumn(Transaction tran, String table, String column) {
		Table tbl = ck_getTable(table);

		int fldnum = Character.isUpperCase(column.charAt(0)) ? -1 : tbl.nextfield;
		if (! column.equals("-")) { // addition of deleted field used by dump/load
			column = column.toLowerCase();
			if (tbl.hasColumn(column))
				throw new SuException("add column: column already exists: " + column + " in " + table);
			Record rec = Column.record(tbl.num, column, fldnum);
			add_any_record(tran, "columns", rec);
			}
		if (fldnum >= 0) {
			++tbl.nextfield;
			tbl.update();
			}
		tables.remove(table);
	}

	public void addIndex(Transaction tran, String table, String columns, boolean isKey, String fktable,
			String fkcolumns, int fkmode, boolean unique, boolean lower) {
		Table tbl = ck_getTable(table);
		short[] colnums = tbl.columns.nums(columns);
		if (tbl.hasIndex(columns))
			throw new SuException("add index: index already exists: " + columns + " in " + table);
		BtreeIndex index = new BtreeIndex(this, tbl.num, columns, isKey, unique);

		if (tbl.hasIndexes() && tbl.hasRecords())
			{
			// insert existing records
			Index idx = tbl.firstIndex();
//			Table fktbl = getTable(fktable);
			for (BtreeIndex.Iter iter = idx.btreeIndex.iter(tran).next(); ! iter.eof(); iter.next())
				{
				Record r = iter.data();
				// if (fkey_source_block(SCHEMA_TRAN, fktbl, fkcolumns,
				// r.project(colnums)))
				// throw new SuException("add index: blocked by foreign key: " +
				// columns + " in " + table);
				Record key = r.project(colnums, iter.cur().keyadr());
				if (! index.insert(tran, new Slot(key)))
					throw new SuException("add index: duplicate key: " + columns + " = " + key + " in " + table);
				}
			}

		add_any_record(tran, "indexes", Index.record(index));
		tables.remove(table);

		if (!fktable.equals(""))
			tables.remove(fktable); // update target
	}

	public Table ck_getTable(String table) {
		Table tbl = getTable(table);
		if (tbl == null)
			throw new SuException("nonexistent table: " + table);
		return tbl;
	}

	private boolean tableExists(String table) {
		return getTable(table) != null;
	}

	public Table getTable(String table) {
		// if the table has already been read, return it
		Table tbl = tables.get(table);
		if (tbl != null) {
			verify(tbl.name.equals(table));
			return tbl;
		}
		return getTable(tablename_index, key(table));
	}

	public Table getTable(int tblnum) {
		// if the table has already been read, return it
		Table tbl = tables.get(tblnum);
		if (tbl != null) {
			verify(tbl.num == tblnum);
			return tbl;
		}
		return getTable(tablenum_index, key(tblnum));
	}

	private Table getTable(BtreeIndex bi, Record key) {
		Transaction tran = Transaction.readonly();
		try {
			Record table_rec = find(tran, bi, key);
			if (table_rec == null)
				return null; // table not found
	
			Table table = new Table(table_rec);
	
			Record tblkey = key(table.num);
	
			// columns
			for (BtreeIndex.Iter iter = columns_index.iter(tran, tblkey)
					.next();
					!iter.eof(); iter.next())
				table.addColumn(new Column(iter.data()));
			table.sortColumns();
	
			// have to do this before indexes since they may need it
	
			// indexes
			for (BtreeIndex.Iter iter = indexes_index.iter(tran, tblkey)
					.next(); !iter.eof(); iter
					.next()) {
				Record r = iter.data();
				String cols = Index.getColumns(r);
				// make sure to use the same index for the system tables
				BtreeIndex index;
				if (table.name.equals("tables") && cols.equals("tablename"))
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
					index = Index.btreeIndex(this, r);
				table.addIndex(new Index(r, cols, table.columns
						.nums(cols), index));
			}
			tables.add(table);
			return table;
		} finally {
			tran.complete();
		}
	}

	public void addRecord(Transaction tran, String table, Record r) {
		if (is_system_table(table))
			throw new SuException("add record: can't add records to system table: " + table);
		add_any_record(tran, table, r);
	}

	private boolean is_system_table(String table) {
		return	table.equals("tables") ||
				table.equals("columns") ||
				table.equals("indexes") ||
				table.equals("views");
	}

	private void add_any_record(Transaction tran, String table, Record r) {
		add_any_record(tran, ck_getTable(table), r);
		}
	private void add_any_record(Transaction tran, Table tbl, Record r) {
		if (tran.isReadonly())
			throw new SuException("can't output from read-only transaction to " + tbl.name);
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
		add_index_entries(tran, tbl, r, adr);
		tran.create_act(tbl.num, adr);

		if (!loading)
			tbl.user_trigger(tran, Record.MINREC, r);
	}

	void add_index_entries(Transaction tran, Table tbl, Record r, long adr) {
		for (Index i : tbl.indexes) {
			Record key = r.project(i.colnums, adr);
			// handle insert failing due to duplicate key
			if (!i.btreeIndex.insert(tran, new Slot(key))) {
				// delete from previous indexes
				for (Index j : tbl.indexes) {
					if (j == i)
						break;
					key = r.project(j.colnums, adr);
					verify(j.btreeIndex.remove(key));
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

	public void removeRecord(Transaction tran, String table, String index, Record key) {
		if (is_system_table(table))
			throw new SuException("delete record: can't delete records from system table: " + table);
		remove_any_record(tran, table, index, key);
	}
	
	private void remove_any_record(Transaction tran, String table, String index, Record key) {
		Table tbl = ck_getTable(table);
		// lookup key in given index
		Index idx = tbl.indexes.find(index);
		verify(idx != null);
		Record rec = find(tran, idx.btreeIndex, key);
		if (rec == null)
			throw new SuException("delete record: can't find record in " + table);
		remove_any_record(tran, tbl, rec);
	}

	public void remove_any_record(Transaction tran, Table tbl, Record r) {
		if (tran.isReadonly())
			throw new SuException("can't delete from read-only transaction in " + tbl.name);
		verify(tbl != null);
		verify(r != null);
	
//		if (char* fktblname = fkey_target_block(tran, tbl, r))
//			throw new SuException("delete record from " + tbl->name + " blocked by foreign key from " + fktblname);
	
		if (! tran.delete_act(tbl.num, r.off()))
			throw new SuException("delete record from " + tbl.name + " transaction conflict: " + tran.conflict());
	
remove_index_entries(tbl, r);

		--tbl.nrecords;
		tbl.totalsize -= r.bufSize();
		tbl.update(); // update tables record
	
		if (! loading)
			tbl.user_trigger(tran, r, Record.MINREC);
	}

	private void remove_index_entries(Table tbl, Record r) {
		long off = r.off();
		for (Index i : tbl.indexes) {
			Record key = r.project(i.colnums, off);
			verify(i.btreeIndex.remove(key));
			i.update(); // update indexes record from index
		}
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

}
