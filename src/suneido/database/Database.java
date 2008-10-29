package suneido.database;

import static java.lang.Math.min;
import static suneido.Suneido.verify;
import static suneido.Util.commasToList;
import static suneido.Util.listToCommas;
import static suneido.database.Transaction.NULLTRAN;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Adler32;

import suneido.SuException;

/**
 * Implements the Suneido database. Uses {@link Mmfile} and {@link BtreeIndex}.
 * Transactions handled by {@link Transaction} and {@link Transactions}.
 *
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved. Licensed under GPLv2.</small></p>
 */
public class Database {
	private final Destination dest;
	private Dbhdr dbhdr;
	private boolean loading = false;
	private final Adler32 cksum = new Adler32();
	private byte output_type = Mmfile.DATA;
	private final Tables tables = new Tables();
	private final Transactions trans = new Transactions(this);
	public static Database theDB;

	private static class TN {
		final static int TABLES = 0, COLUMNS = 1, INDEXES = 2, VIEWS = 3;
	}
	private static class V {
		final static int NAME = 0, DEFINITION = 1;
	}

	private final static int VERSION = 1;
	private BtreeIndex tablename_index;
	private BtreeIndex tablenum_index;
	private BtreeIndex columns_index;
	private BtreeIndex indexes_index;
	private BtreeIndex fkey_index;
	private BtreeIndex views_index;

	public Database(String filename, Mode mode) {
		dest = new Mmfile(filename, mode);
		init(mode);
	}

	public Database(Destination dest, Mode mode) {
		this.dest = dest;
		init(mode);
	}

	private void init(Mode mode) {
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
		} else
			open();
	}

	private void create() {
		loading = true;

		long dbhdr_at = alloc(Dbhdr.SIZE, Mmfile.OTHER);

		// tables
		tablename_index = new BtreeIndex(dest, TN.TABLES, "tablename", true,
				false);
		tablenum_index = new BtreeIndex(dest, TN.TABLES, "table", true, false);
		createSchemaTable("tables", TN.TABLES, 5, 3);
		createSchemaTable("columns", TN.COLUMNS, 3, 17);
		createSchemaTable("indexes", TN.INDEXES, 9, 5);

		// columns
		columns_index = new BtreeIndex(dest, TN.COLUMNS, "table,column", true,
				false);
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
		indexes_index = new BtreeIndex(dest, TN.INDEXES, "table,columns",
				true, false);
		fkey_index = new BtreeIndex(dest, TN.INDEXES, "fktable,fkcolumns",
				false, false);
		createSchemaIndex(tablename_index);
		createSchemaIndex(tablenum_index);
		createSchemaIndex(columns_index);
		// output indexes indexes last
		long indexes_adr = createSchemaIndex(indexes_index);
		createSchemaIndex(fkey_index);

		dbhdr = new Dbhdr(dbhdr_at, indexes_adr);

		loading = false;

		// views
		addTable("views");
		addColumn("views", "view_name");
		addColumn("views", "view_definition");
		addIndex("views", "view_name", true);
		views_index = btreeIndex(TN.VIEWS, "view_name");
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
		Record key1 = new Record().add(btreeIndex.tblnum).add(btreeIndex.indexColumns)
		.addMmoffset(at);
		verify(indexes_index.insert(NULLTRAN, new Slot(key1)));
		Record key2 = new Record().add("").add("").addMmoffset(at);
		verify(fkey_index.insert(NULLTRAN, new Slot(key2)));
		return at;
	}

	private void open() {
		dbhdr = new Dbhdr();
		Session.startup(dest);
		dest.sync();
		indexes_index = Index.btreeIndex(dest, input(dbhdr.indexes));

		Record r = find(NULLTRAN, indexes_index,
				key(TN.INDEXES, "table,columns"));
		verify(!r.isEmpty() && r.off() == dbhdr.indexes);

		tablename_index = btreeIndex(TN.TABLES, "tablename");
		tablenum_index = btreeIndex(TN.TABLES, "table");
		columns_index = btreeIndex(TN.COLUMNS, "table,column");
		fkey_index = btreeIndex(TN.INDEXES, "fktable,fkcolumns");
		// WARNING: any new indexes added here must also be added in get_table
		views_index = btreeIndex(TN.VIEWS, "view_name");
	}
	private BtreeIndex btreeIndex(int table_num, String columns) {
		return Index.btreeIndex(dest,
				find(NULLTRAN, indexes_index, key(table_num, columns)));
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
	void checksum(ByteBuffer buf, int len) {
		buf.position(0);
		for (int i = 0; i < len; i += bytes.length) {
			int n = min(bytes.length, len - i);
			buf.get(bytes, 0, n);
			cksum.update(bytes, 0, n);
		}
	}

	void resetChecksum() {
		cksum.reset();
	}

	int getChecksum() {
		return (int) cksum.getValue();
	}

	public Record input(long adr) {
		verify(adr != 0);
		return new Record(adr(adr), adr);
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

	private Record find(Transaction tran, Index index, Record key) {
		return find(tran, index.btreeIndex, key);
	}

	private Record find(Transaction tran, BtreeIndex btreeIndex, Record key) {
		Slot slot = btreeIndex.find(tran, key);
		return slot == null ? null : input(slot.keyadr());
	}

	public void close() {
		trans.shutdown();
		Session.shutdown(dest);
		dest.close();
	}

	public Transaction readonlyTran() {
		return Transaction.readonly(trans);
	}
	public Transaction readwriteTran() {
		return Transaction.readwrite(trans);
	}

	// tables =======================================================

	public void addTable(String table) {
		if (tableExists(table))
			throw new SuException("add table: table already exists: " + table);
		int tblnum = dbhdr.next_table++;
		Transaction tran = readwriteTran();
		try {
			Record r = Table.record(table, tblnum, 0, 0);
			add_any_record(tran, "tables", r);
			tran.complete();
		} finally {
			tran.abortIfNotComplete();
		}
	}

	public void removeTable(String tablename) {
		if (is_system_table(tablename))
			throw new SuException("drop: can't destroy system table: "
					+ tablename);
		Table table = ck_getTable(tablename);
		Transaction tran = readwriteTran();
		try {
			for (Index index : table.indexes)
				removeIndex(tran, table, index.columns);
			for (Column column : table.columns)
				removeColumn(tran, table, column.name);
			remove_any_record(tran, "tables", "tablename", key(tablename));
			tran.ck_complete();
		} finally {
			tran.abortIfNotComplete();
		}
		tables.remove(tablename);
	}

	public void renameTable(String oldname, String newname) {
		if (oldname.equals(newname))
			return ;

		Table tbl = ck_getTable(oldname);
		if (is_system_table(oldname))
			throw new SuException("rename table: can't rename system table: " + oldname);
		if (null != getTable(newname))
			throw new SuException("rename table: table already exists: " + newname);

		Transaction tran = readwriteTran();
		try {
			update_any_record(tran, "tables", "table", key(tbl.num),
				Table.record(newname, tbl.num, tbl.nextfield, tbl.nrecords));
			tran.ck_complete();
		} finally {
			tran.abortIfNotComplete();
		}

		tables.remove(oldname);

	}

	// columns ======================================================

	public void addColumn(String tablename, String column) {
		Table table = ck_getTable(tablename);

		int fldnum = Character.isUpperCase(column.charAt(0)) ? -1
				: table.nextfield;
		if (! column.equals("-")) { // addition of deleted field used by dump/load
			column = column.toLowerCase();
			if (table.hasColumn(column))
				throw new SuException("add column: column already exists: "
						+ column + " in " + tablename);
			Transaction tran = readwriteTran();
			try {
				Record rec = Column.record(table.num, column, fldnum);
				add_any_record(tran, "columns", rec);
				tran.complete();
			} finally {
				tran.abortIfNotComplete();
			}
		}
		if (fldnum >= 0) {
			++table.nextfield;
			table.update();
		}
		tables.remove(tablename);
	}

	public void removeColumn(String tablename, String name) {
		if (is_system_column(tablename, name))
			throw new SuException("delete column: can't delete system column: "
					+ name + " from " + tablename);

		Table tbl = ck_getTable(tablename);

		if (tbl.columns.find(name) == null)
			throw new SuException("delete column: nonexistent column: " + name
					+ " in " + tablename);

		for (Index index : tbl.indexes)
			if (index.hasColumn(name))
				throw new SuException(
						"delete column: can't delete column used in index: "
						+ name + " in " + tablename);

		Transaction tran = readwriteTran();
		try {
			removeColumn(tran, tbl, name);
			tran.ck_complete();
		} finally {
			tran.abortIfNotComplete();
		}
		tables.remove(tablename);
	}

	private void removeColumn(Transaction tran, Table tbl, String name) {
		remove_any_record(tran, "columns", "table,column", key(tbl.num, name));
	}

	public void renameColumn(String table, String oldname, String newname) {
		if (oldname == newname)
			return ;

		Table tbl = ck_getTable(table);
		if (is_system_column(table, oldname))
			throw new SuException("rename column: can't rename system column: "
					+ oldname + " in " + table);

		Column col = tbl.getColumn(oldname);
		if (col == null)
			throw new SuException("rename column: nonexistent column: "
					+ oldname + " in " + table);
		if (tbl.hasColumn(newname))
			throw new SuException("rename column: column already exists: "
					+ newname + " in " + table);

		Transaction tran = readwriteTran();
		try {
			update_any_record(tran, "columns", "table,column",
					key(tbl.num, oldname),
					Column.record(tbl.num, newname, col.num));

			// update any indexes that include this column
			for (Index idx : tbl.indexes) {
				List<String> cols = commasToList(idx.columns);
				int i = cols.indexOf(oldname);
				if (i < 0)
					continue ; // this index doesn't contain the column
				cols.set(i, newname);
				idx.btreeIndex.indexColumns = listToCommas(cols);
				update_any_record(tran, "indexes", "table,columns",
						key(tbl.num, idx.columns), idx.record());
				}
			tran.ck_complete();
		} finally {
			tran.abortIfNotComplete();
		}
		tables.remove(table);
	}

	// indexes ======================================================

	public void addIndex(String tablename, String columns, boolean isKey) {
		addIndex(tablename, columns, isKey, false, false, null, null, 0);
	}

	public void addIndex(String tablename, String columns,
			boolean isKey, boolean unique, boolean lower,
			String fktable,	String fkcolumns, int fkmode) {
		Table table = ck_getTable(tablename);
		short[] colnums = table.columns.nums(columns);
		if (table.hasIndex(columns))
			throw new SuException("add index: index already exists: " + columns
					+ " in " + tablename);
		BtreeIndex index = new BtreeIndex(dest, table.num, columns, isKey,
				unique);

		Transaction tran = readwriteTran();
		try {
			insertExistingRecords(tran, columns, table, colnums,
					fktable, fkcolumns, index);
			add_any_record(tran, "indexes",
					Index.record(index, fktable, fkcolumns, fkmode));
			tran.complete();
		} finally {
			tran.abortIfNotComplete();
		}
		tables.remove(tablename);

		if (fktable != null)
			tables.remove(fktable); // update target
	}

	private void insertExistingRecords(Transaction tran, String columns,
			Table table, short[] colnums, String fktable, String fkcolumns,
			BtreeIndex index) {
		if (!table.hasIndexes() || !table.hasRecords())
			return ;
		Index idx = table.firstIndex();
		Table fktbl = getTable(fktable);
		for (BtreeIndex.Iter iter = idx.btreeIndex.iter(tran).next();
				!iter.eof(); iter.next()) {
			Record rec = input(iter.keyadr());
			fkey_source_block1(tran, fktbl, fkcolumns, rec.project(colnums),
					"add index to " + table.name);
			Record key = rec.project(colnums, iter.cur().keyadr());
			if (!index.insert(tran, new Slot(key)))
				throw new SuException("add index: duplicate key: " + columns
						+ " = " + key + " in " + table.name);
		}
	}

	public void removeIndex(String tablename, String columns) {
		if (is_system_index(tablename, columns))
			throw new SuException("delete index: can't delete system index: "
					+ columns + " from " + tablename);
		Table tbl = ck_getTable(tablename);
		if (tbl.indexes.size() == 1)
			throw new SuException("delete index: can't delete last index from "
					+ tablename);
		Transaction tran = readwriteTran();
		try {
			removeIndex(tran, tbl, columns);
			tran.ck_complete();
		} finally {
			tran.abortIfNotComplete();
		}
		tables.remove(tablename);
	}

	private void removeIndex(Transaction tran, Table tbl, String columns) {
		if (!tbl.indexes.hasIndex(columns))
			throw new SuException("delete index: nonexistent index: " + columns
					+ " in " + tbl.name);

		remove_any_record(tran, "indexes", "table,columns",
				key(tbl.num, columns));
	}

	private boolean tableExists(String table) {
		return getTable(table) != null;
	}

	public Table ck_getTable(String tablename) {
		Table tbl = getTable(tablename);
		if (tbl == null)
			throw new SuException("nonexistent table: " + tablename);
		return tbl;
	}

	public Table getTable(String tablename) {
		// if the table has already been read, return it
		Table table = tables.get(tablename);
		if (table != null) {
			verify(table.name.equals(tablename));
			return table;
		}
		return getTable(tablename_index, key(tablename));
	}

	public Table ck_getTable(int tblnum) {
		Table tbl = getTable(tblnum);
		if (tbl == null)
			throw new SuException("nonexistent table: " + tblnum);
		return tbl;
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
		Transaction tran = readonlyTran();
		try {
			Record table_rec = find(tran, bi, key);
			if (table_rec == null)
				return null; // table not found

			Table table = new Table(table_rec);

			Record tblkey = key(table.num);

			// columns
			for (BtreeIndex.Iter iter = columns_index.iter(tran, tblkey).next();
			!iter.eof(); iter.next())
				table.addColumn(new Column(input(iter.keyadr())));
			table.sortColumns();

			// indexes
			for (BtreeIndex.Iter iter = indexes_index.iter(tran, tblkey)
					.next(); !iter.eof(); iter
					.next()) {
				Record r = input(iter.keyadr());
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
					index = Index.btreeIndex(dest, r);
				table.addIndex(new Index(r, cols, table.columns
						.nums(cols),
						index, getForeignKeys(tran, table, cols)));
			}
			tables.add(table);
			return table;
		} finally {
			tran.complete();
		}
	}

	// find foreign keys pointing to this index
	private List<Record> getForeignKeys(Transaction tran, Table table,
			String columns) {
		List<Record> records = new ArrayList<Record>();
		for (BtreeIndex.Iter iter = fkey_index.iter(tran,
				key(table.name, columns)).next(); !iter.eof(); iter.next())
			records.add(input(iter.keyadr()));
		return records;
	}

	private Record key(String name, String columns) {
		return new Record().add(name).add(columns);
	}

	public Index getIndex(Table table, String indexcolumns) {
		return table == null ? null : table.getIndex(indexcolumns);
	}

	public String schema(String table) {
		return ck_getTable(table).schema();
	}

	// views ========================================================

	public void add_view(String table, String definition) {
		Transaction tran = readwriteTran();
		try {
			add_any_record(tran, "views",
					new Record().add(table).add(definition));
			tran.ck_complete();
		} finally {
			tran.abortIfNotComplete();
		}
	}

	public String getView(String viewname) {
		Record rec;
		Transaction tran = readonlyTran();
		try {
			rec = find(tran, views_index, key(viewname));
		} finally {
			tran.complete();
		}
		return rec == null ? "" : rec.getString(V.DEFINITION);
	}

	public void removeView(String viewname) {
		Transaction tran = readwriteTran();
		try {
			remove_any_record(tran, "views", "view_name", key(viewname));
			tran.ck_complete();
		} finally {
			tran.abortIfNotComplete();
		}
	}

	// add record ===================================================

	public void addRecord(Transaction tran, String table, Record r) {
		if (is_system_table(table))
			throw new SuException("add record: can't add records to system table: " + table);
		add_any_record(tran, table, r);
	}

	private boolean is_system_table(String table) {
		return table.equals("tables") || table.equals("columns")
		|| table.equals("indexes") || table.equals("views");
	}

	private boolean is_system_column(String table, String column) {
		return (table.equals("tables") && (column.equals("table")
				|| column.equals("nrows") || column.equals("totalsize")))
				|| (table.equals("columns") && (column.equals("table")
						|| column.equals("column") || column.equals("field")))
						|| (table.equals("indexes") && (column.equals("table")
								|| column.equals("columns") || column.equals("root")
								|| column.equals("treelevels") || column.equals("nnodes")));
	}

	private boolean is_system_index(String table, String columns) {
		return (table.equals("tables") && columns.equals("table"))
		|| (table.equals("columns") && columns.equals("table,column"))
		|| (table.equals("indexes") && columns.equals("table,columns"));
	}

	private void add_any_record(Transaction tran, String table, Record r) {
		add_any_record(tran, ck_getTable(table), r);
	}
	private void add_any_record(Transaction tran, Table table, Record rec) {
		if (tran.isReadonly())
			throw new SuException("can't output from read-only transaction to "
					+ table.name);
		verify(table != null);
		verify(!table.indexes.isEmpty());

		if (!loading)
			fkey_source_block(tran, table, rec, "add record to " + table.name);

		long adr = output(table.num, rec);
		add_index_entries(tran, table, rec, adr);
		tran.create_act(table.num, adr);

		if (!loading)
			table.user_trigger(tran, Record.MINREC, rec);
	}

	void add_index_entries(Transaction tran, Table table, Record rec, long adr) {
		for (Index i : table.indexes) {
			Record key = rec.project(i.colnums, adr);
			// handle insert failing due to duplicate key
			if (!i.btreeIndex.insert(tran, new Slot(key))) {
				// delete from previous indexes
				for (Index j : table.indexes) {
					if (j == i)
						break;
					key = rec.project(j.colnums, adr);
					verify(j.btreeIndex.remove(key));
				}
				throw new SuException("duplicate key: " + i.columns + " = "
						+ key + " in " + table.name);
			}
			i.update(); // update indexes record from index
		}

		++table.nrecords;
		table.totalsize += rec.bufSize();
		table.update(); // update tables record
	}

	// update record ================================================

	public long updateRecord(Transaction tran, long recadr, Record rec) {
		verify(recadr > 0);
		int tblnum = adr(recadr - 4).getInt(0);
		Table tbl = ck_getTable(tblnum);
		return update_record(tran, tbl, input(recadr), rec, true);
		// TODO: should be checking for system table
	}

	public void updateRecord(Transaction tran, String table, String index,
			Record key, Record newrec) {
		if (is_system_table(table))
			throw new SuException("can't update records in system table: "
					+ table);
		update_any_record(tran, table, index, key, newrec);
	}

	void update_any_record(Transaction tran, String tablename,
			String indexcolumns, Record key, Record newrec) {
		Table table = ck_getTable(tablename);
		Index index = getIndex(table, indexcolumns);
		Record oldrec = find(tran, index, key);
		if (oldrec == null)
			throw new SuException("update record: can't find record in "
					+ tablename);

		update_record(tran, table, oldrec, newrec, true);
	}

	long update_record(Transaction tran, Table table, Record oldrec,
			Record newrec, boolean srcblock) {
		if (tran.isReadonly())
			throw new SuException("can't update from read-only transaction in "
					+ table.name);

		long oldoff = oldrec.off();

		// check foreign keys
		for (Index i : table.indexes) {
			if ((!srcblock || i.fksrc == null) && i.fkdsts.isEmpty())
				continue; // no foreign keys for this index
			Record oldkey = oldrec.project(i.colnums);
			Record newkey = newrec.project(i.colnums);
			if (oldkey.equals(newkey))
				continue;
			if (srcblock && i.fksrc != null)
				fkey_source_block1(tran, getTable(i.fksrc.tablename),
						i.fksrc.columns,
						newkey, "update record in " + table.name);
			fkey_target_block1(tran, i, oldkey, newkey, "update record in "
					+ table.name);
		}

		if (!tran.delete_act(table.num, oldoff))
			throw new SuException("update record in " + table.name
					+ " transaction conflict: " + tran.conflict());

		// do the update
		long newoff = output(table.num, newrec); // output new version

		// update indexes
		for (Index i : table.indexes) {
			Record newkey = newrec.project(i.colnums, newoff);
			if (!i.btreeIndex.insert(tran, new Slot(newkey))) {
				// undo previous
				for (Index j : table.indexes)
					verify(j.btreeIndex.remove(
							newrec.project(j.colnums, newoff)));
				tran.undo_delete_act(table.num, oldoff);
				throw new SuException("update record: duplicate key: "
						+ i.columns + " = " + newkey + " in " + table.name);
			}
			i.update();
		}
		tran.create_act(table.num, newoff);
		table.totalsize += newrec.bufSize() - oldrec.bufSize();
		table.update();

		table.user_trigger(tran, oldrec, newrec);
		return newoff;
	}

	// remove record ================================================

	public void removeRecord(Transaction tran, long recadr) {
		verify(recadr > 0);
		int tblnum = adr(recadr - 4).getInt(0);
		Table tbl = ck_getTable(tblnum);
		remove_any_record(tran, tbl, input(recadr));
		// TODO: should be checking for system table

	}

	public void removeRecord(Transaction tran, String tablename, String index,
			Record key) {
		if (is_system_table(tablename))
			throw new SuException(
					"delete record: can't delete records from system table: "
					+ tablename);
		remove_any_record(tran, tablename, index, key);
	}

	private void remove_any_record(Transaction tran, String tablename,
			String indexcolumns, Record key) {
		Table table = ck_getTable(tablename);
		// lookup key in given index
		Index index = table.indexes.get(indexcolumns);
		verify(index != null);
		Record rec = find(tran, index.btreeIndex, key);
		if (rec == null)
			throw new SuException("delete record: can't find record in "
					+ tablename);
		remove_any_record(tran, table, rec);
	}

	public void remove_any_record(Transaction tran, Table table, Record rec) {
		if (tran.isReadonly())
			throw new SuException("can't delete from read-only transaction in "
					+ table.name);
		verify(table != null);
		verify(rec != null);

		fkey_target_block(tran, table, rec, "delete from " + table.name);

		if (!tran.delete_act(table.num, rec.off()))
			throw new SuException("delete record from " + table.name
					+ " transaction conflict: " + tran.conflict());

		--table.nrecords;
		table.totalsize -= rec.bufSize();
		table.update(); // update tables record

		if (!loading)
			table.user_trigger(tran, rec, Record.MINREC);
	}

	// foreign keys =================================================

	private void fkey_source_block(Transaction tran, Table table, Record rec, String action) {
		for (Index index : table.indexes)
			if (index.fksrc != null)
				fkey_source_block1(tran, getTable(index.fksrc.tablename),
						index.fksrc.columns, rec.project(index.colnums), action);
	}

	private void fkey_source_block1(Transaction tran, Table fktbl,
			String fkcolumns, Record key, String action) {
		if (fkcolumns == "" || key.allEmpty())
			return;
		Index fkidx = getIndex(fktbl, fkcolumns);
		if (fkidx == null || find(tran, fkidx, key) == null)
			throw new SuException(action + " blocked by foreign key to " + fktbl.name + " ");
	}

	void fkey_target_block(Transaction tran, Table tbl, Record r, String action) {
		for (Index i : tbl.indexes)
			fkey_target_block1(tran, i, r.project(i.colnums), null, action);
	}

	private void fkey_target_block1(Transaction tran, Index index, Record key,
			Record newkey, String action) {
		if (key.allEmpty())
			return;
		for (Index.ForeignKey fk : index.fkdsts) {
			Table fktbl = getTable(fk.tblnum);
			Index fkidx = getIndex(fktbl, fk.columns);
			if (fkidx == null)
				continue ;
			BtreeIndex.Iter iter = fkidx.btreeIndex.iter(tran, key).next();
			if (newkey == null && (fk.mode & Index.CASCADE_DELETES) != 0)
				for (; ! iter.eof(); iter.next())
					cascade_delete(tran, fktbl, iter);
			else if (newkey != null && (fk.mode & Index.CASCADE_UPDATES) != 0)
				for (; !iter.eof(); iter.next())
					cascade_update(tran, newkey, fktbl, iter, fkidx.colnums);
			else // blocking
				if (! iter.eof())
					throw new SuException(action + " blocked by foreign key in "
							+ fktbl.name);
		}
	}

	private void cascade_update(Transaction tran, Record newkey, Table fktbl,
			BtreeIndex.Iter iter, short[] colnums) {
		Record oldrec = input(iter.keyadr());
		Record newrec = new Record();
		for (int i = 0; i < oldrec.size(); ++i) {
			int j = 0;
			for (; j < colnums.length && colnums[j] != i; ++j)
				;
			if (j >= colnums.length)
				newrec.add(oldrec.get(i));
			else
				newrec.add(newkey.get(j));
		}
		update_record(tran, fktbl, oldrec, newrec, false);
	}

	private void cascade_delete(Transaction tran, Table fktbl,
			BtreeIndex.Iter iter) {
		Record r = input(iter.keyadr());
		remove_any_record(tran, fktbl, r);
		iter.reset_prevsize();
		// need to reset prevsize in case trigger updates other lines
		// otherwise iter doesn't "see" the updated lines
	}

	// used by Transaction.abort
	void undoAdd(int tblnum, long adr) {
		Table table = getTable(tblnum);
		if (table == null)
			return;
		Record rec = input(adr);
		remove_index_entries(table, rec);
		--table.nrecords;
		table.totalsize -= rec.bufSize();
		table.update();
	}

	// used by Transaction.abort
	void undoDelete(int tblnum, long adr) {
		Table table = getTable(tblnum);
		if (table == null)
			return;
		// undo tables record update
		++table.nrecords;
		table.totalsize += input(adr).bufSize();
		table.update();
	}

	// used by Transaction.finalization
	void remove_index_entries(int tblnum, long adr) {
		Table table = getTable(tblnum);
		if (table != null)
			remove_index_entries(table, input(adr));
	}

	private void remove_index_entries(Table table, Record rec) {
		long off = rec.off();
		for (Index index : table.indexes) {
			Record key = rec.project(index.colnums, off);
			verify(index.btreeIndex.remove(key));
			index.update(); // update indexes record from index
		}
	}

	public long alloc(int n, byte type) {
		return dest.alloc(n, type);
	}

	public ByteBuffer adr(long offset) {
		return dest.adr(offset);
	}

	static byte[] bytes = new byte[256];

	private class Dbhdr {
		static final int SIZE = 4 + 4 + 4;
		ByteBuffer buf;
		int next_table;
		long indexes;

		// create
		Dbhdr(long at, long indexes_adr) {
			verify(at == dest.first());
			buf = adr(at);
			buf.putInt(next_table = TN.INDEXES + 1);
			buf.putInt(Mmfile.offsetToInt(indexes = indexes_adr));
			buf.putInt(VERSION);
		}

		// open
		Dbhdr() {
			long at = dest.first();
			if (dest.length(at) < SIZE)
				throw new SuException("invalid database");
			buf = adr(dest.first());
			next_table = buf.getInt();
			indexes = Mmfile.intToOffset(buf.getInt());
			int version = buf.getInt();
			if (version != VERSION)
				throw new SuException("invalid database");
		}
	}

}
