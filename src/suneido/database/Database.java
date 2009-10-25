package suneido.database;

import static java.lang.Math.min;
import static suneido.Suneido.verify;
import static suneido.database.Transaction.NULLTRAN;
import static suneido.util.Util.commasToList;
import static suneido.util.Util.listToCommas;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.Adler32;

import suneido.SuException;
import suneido.util.PersistentMap;

import com.google.common.collect.ImmutableList;

/**
 * Implements the Suneido database. Uses {@link Mmfile} and {@link BtreeIndex}.
 * Transactions handled by {@link Transaction} and {@link Transactions}.
 *
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
public class Database {
	private final Destination dest;
	private Dbhdr dbhdr;
	private boolean loading = false;
	private final Adler32 cksum = new Adler32();
	private byte output_type = Mmfile.DATA;
	public volatile Tables tables = new Tables();
	public volatile PersistentMap<Integer, TableData> tabledata = null;
	public volatile PersistentMap<String, BtreeIndex> btreeIndexes =
			PersistentMap.empty();
	private final Transactions trans = new Transactions(this);
	public static Database theDB;

	private static class TN {
		final static int TABLES = 1, COLUMNS = 2, INDEXES = 3, VIEWS = 4;
	}
	private static class V {
		@SuppressWarnings("unused")
		final static int NAME = 0, DEFINITION = 1;
	}
	private final static int VERSION = 1;

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
		loadSchema();
	}

	private void create() {
		loading = true;

		long dbhdr_at = alloc(Dbhdr.SIZE, Mmfile.OTHER);

		// tables
		BtreeIndex tablenum_index = new BtreeIndex(dest, TN.TABLES, "table",
				true, false);
		BtreeIndex tablename_index = new BtreeIndex(dest, TN.TABLES, "tablename",
				true, false);
		createSchemaTable(tablenum_index, tablename_index, "tables", TN.TABLES, 5, 3);
		createSchemaTable(tablenum_index, tablename_index, "columns", TN.COLUMNS, 3, 17);
		createSchemaTable(tablenum_index, tablename_index, "indexes", TN.INDEXES, 9, 5);

		// columns
		BtreeIndex columns_index = new BtreeIndex(dest, TN.COLUMNS, "table,column",
				true, false);
		createSchemaColumn(columns_index, TN.TABLES, "table", 0);
		createSchemaColumn(columns_index, TN.TABLES, "tablename", 1);
		createSchemaColumn(columns_index, TN.TABLES, "nextfield", 2);
		createSchemaColumn(columns_index, TN.TABLES, "nrows", 3);
		createSchemaColumn(columns_index, TN.TABLES, "totalsize", 4);

		createSchemaColumn(columns_index, TN.COLUMNS, "table", 0);
		createSchemaColumn(columns_index, TN.COLUMNS, "column", 1);
		createSchemaColumn(columns_index, TN.COLUMNS, "field", 2);

		createSchemaColumn(columns_index, TN.INDEXES, "table", 0);
		createSchemaColumn(columns_index, TN.INDEXES, "columns", 1);
		createSchemaColumn(columns_index, TN.INDEXES, "key", 2);
		createSchemaColumn(columns_index, TN.INDEXES, "fktable", 3);
		createSchemaColumn(columns_index, TN.INDEXES, "fkcolumns", 4);
		createSchemaColumn(columns_index, TN.INDEXES, "fkmode", 5);
		createSchemaColumn(columns_index, TN.INDEXES, "root", 6);
		createSchemaColumn(columns_index, TN.INDEXES, "treelevels", 7);
		createSchemaColumn(columns_index, TN.INDEXES, "nnodes", 8);

		// indexes
		BtreeIndex indexes_index = new BtreeIndex(dest, TN.INDEXES, "table,columns",
				true, false);
		BtreeIndex fkey_index = new BtreeIndex(dest, TN.INDEXES, "fktable,fkcolumns",
				false, false);
		createSchemaIndex(indexes_index, fkey_index, tablename_index);
		createSchemaIndex(indexes_index, fkey_index, tablenum_index);
		createSchemaIndex(indexes_index, fkey_index, columns_index);
		// output indexes indexes last
		long indexes_adr = createSchemaIndex(indexes_index, fkey_index, indexes_index);
		createSchemaIndex(indexes_index, fkey_index, fkey_index);

		dbhdr = new Dbhdr(dbhdr_at, indexes_adr);

		loading = false;

		// views
		getIndexes();
		loadSchema(); // need to load what we have so far
		addTable("views");
		addColumn("views", "view_name");
		addColumn("views", "view_definition");
		addIndex("views", "view_name", true);
	}

	private void createSchemaTable(
			BtreeIndex tablenum_index, BtreeIndex tablename_index,
			String name, int num, int nextfield, int nrecords) {
		long at = output(TN.TABLES, Table.record(name, num, nextfield, nrecords));
		verify(tablenum_index.insert(NULLTRAN,
				new Slot(new Record().add(num).addMmoffset(at))));
		verify(tablename_index.insert(NULLTRAN,
				new Slot(new Record().add(name).addMmoffset(at))));
	}

	private void createSchemaColumn(BtreeIndex columns_index,
			int tblnum, String column, int field) {
		long at = output(TN.COLUMNS, Column.record(tblnum, column, field));
		Record key = new Record().add(tblnum).add(column).addMmoffset(at);
		verify(columns_index.insert(NULLTRAN, new Slot(key)));
	}

	private long createSchemaIndex(BtreeIndex indexes_index, BtreeIndex fkey_index,
			BtreeIndex btreeIndex) {
		long at = output(TN.INDEXES, btreeIndex.record);
		Record key1 = new Record()
				.add(btreeIndex.tblnum)
				.add(btreeIndex.columns)
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
		getIndexes();
	}

	private void getIndexes() {
		BtreeIndex indexes_index = new BtreeIndex(dest, input(dbhdr.indexes));
		btreeIndexes = btreeIndexes.with(TN.INDEXES + ":table,columns", indexes_index);

		Record r = find(NULLTRAN, indexes_index,
				key(TN.INDEXES, "table,columns"));
		verify(!r.isEmpty() && r.off() == dbhdr.indexes);

		btreeIndex(indexes_index, TN.TABLES, "table");
		btreeIndex(indexes_index, TN.COLUMNS, "table,column");
		btreeIndex(indexes_index, TN.INDEXES, "fktable,fkcolumns");
	}
	private void btreeIndex(BtreeIndex indexes_index,
			int table_num, String columns) {
		BtreeIndex bti = new BtreeIndex(dest,
				find(NULLTRAN, indexes_index, key(table_num, columns)));
		btreeIndexes = btreeIndexes.with(table_num + ":" + columns, bti);
	}

	private void loadSchema() {
		Tables.Builder tablesBuilder = new Tables.Builder();
		PersistentMap.Builder<Integer, TableData> tabledataBuilder =
				PersistentMap.builder();
		PersistentMap.Builder<String, BtreeIndex> btreeIndexBuilder =
				PersistentMap.builder();
		List<BtreeIndex> btis = new ArrayList<BtreeIndex>();
		Transaction tran = readonlyTran();
		try {
			BtreeIndex tablenum_index = tran.getBtreeIndex(TN.TABLES, "table");
			for (BtreeIndex.Iter iter = tablenum_index.iter(tran).next();
					!iter.eof(); iter.next()) {
				Record table_rec = input(iter.keyadr());
				Table table = loadTable(tran, table_rec, btis);
				tablesBuilder.add(table);
				tabledataBuilder.put(table.num, new TableData(table_rec));
				for (BtreeIndex bti : btis)
					btreeIndexBuilder.put(bti.tblnum + ":" + bti.columns, bti);
				btis.clear();
			}
		} finally {
			tran.complete();
		}
		tables = tablesBuilder.build();
		tabledata = tabledataBuilder.build();
		btreeIndexes = btreeIndexBuilder.build();
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
		return find(tran, tran.getBtreeIndex(index), key);
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
		return new Transaction(trans, true, tables, tabledata, btreeIndexes);
	}
	public Transaction readwriteTran() {
		return new Transaction(trans, false, tables, tabledata, btreeIndexes);
	}
	public Transaction cursorTran() {
		return new Transaction(trans, tables, tabledata, btreeIndexes);
	}

	// tables =======================================================

	public void addTable(String tablename) {
		int tblnum = dbhdr.getNextTableNum();
		Transaction tran = readwriteTran();
		try {
			if (tran.tableExists(tablename))
				throw new SuException("add table: table already exists: " + tablename);
			Record r = Table.record(tablename, tblnum, 0, 0);
			add_any_record(tran, "tables", r);
			tran.updateTable(getUpdatedTable(tran, tblnum));
			tran.complete();
		} finally {
			tran.abortIfNotComplete();
		}
	}

	/** returns false if table does not exist */
	public boolean removeTable(String tablename) {
		checkForSystemTable(tablename, "drop");
		Transaction tran = readwriteTran();
		try {
			if (getView(tran, tablename) != null) {
				removeView(tran, tablename);
			} else {
				Table table = tran.ck_getTable(tablename);
				if (table == null)
					return false;
				for (Index index : table.indexes)
					removeIndex(tran, table, index.columns);
				for (Column column : table.columns)
					removeColumn(tran, table, column.name);
				remove_any_record(tran, "tables", "tablename", key(tablename));
				tran.deleteTable(table);
			}
			tran.ck_complete();
		} finally {
			tran.abortIfNotComplete();
		}
		return true;
	}

	private void checkForSystemTable(String tablename, String operation) {
		if (is_system_table(tablename))
			throw new SuException("can't " + operation +
					" system table: " + tablename);
	}

	public void renameTable(String oldname, String newname) {
		if (oldname.equals(newname))
			return ;

		checkForSystemTable(oldname, "rename");

		Transaction tran = readwriteTran();
		try {
			if (tran.tableExists(newname))
				throw new SuException("rename table: table already exists: " + newname);
			Table table = tran.ck_getTable(oldname);
			TableData td = tran.getTableData(table.num);
			update_any_record(tran, "tables", "table", key(table.num),
					Table.record(newname, table.num, td.nextfield, td.nrecords));
			tran.updateTable(getUpdatedTable(tran, table.num));
			tran.ck_complete();
		} finally {
			tran.abortIfNotComplete();
		}
	}

	// columns ======================================================

	public void addColumn(String tablename, String column) {
		Transaction tran = readwriteTran();
		try {
			Table table = tran.ck_getTable(tablename);
			TableData td = tran.getTableData(table.num);
			int fldnum =
					Character.isUpperCase(column.charAt(0)) ? -1 : td.nextfield;
			// "-" is addition of deleted field used by dump/load
			if (!column.equals("-")) {
				if (fldnum == -1)
					column = column.substring(0, 1).toLowerCase()
							+ column.substring(1);
				if (table.hasColumn(column))
					throw new SuException("add column: column already exists: "
							+ column + " in " + tablename);
				Record rec = Column.record(table.num, column, fldnum);
				add_any_record(tran, "columns", rec);
				tran.updateTable(getUpdatedTable(tran, table.num));
			}
			if (fldnum >= 0)
				tran.updateTableData(td.withField());
			tran.complete();
		} finally {
			tran.abortIfNotComplete();
		}
	}

	public void removeColumn(String tablename, String name) {
		if (is_system_column(tablename, name))
			throw new SuException("delete column: can't delete system column: "
					+ name + " from " + tablename);

		Transaction tran = readwriteTran();
		try {
			Table table = tran.ck_getTable(tablename);
			if (table.columns.find(name) == null)
				throw new SuException("delete column: nonexistent column: " + name
						+ " in " + tablename);
			for (Index index : table.indexes)
				if (index.hasColumn(name))
					throw new SuException(
							"delete column: can't delete column used in index: "
							+ name + " in " + tablename);

			removeColumn(tran, table, name);
			tran.updateTable(getUpdatedTable(tran, table.num));
			tran.ck_complete();
		} finally {
			tran.abortIfNotComplete();
		}
	}

	// used by removeColumn and removeTable
	private void removeColumn(Transaction tran, Table tbl, String name) {
		remove_any_record(tran, "columns", "table,column", key(tbl.num, name));
	}

	public void renameColumn(String tablename, String oldname, String newname) {
		if (oldname.equals(newname))
			return ;

		if (is_system_column(tablename, oldname))
			throw new SuException("rename column: can't rename system column: "
					+ oldname + " in " + tablename);

		Transaction t = readwriteTran();
		try {
			Table table = t.ck_getTable(tablename);
			if (table.hasColumn(newname))
				throw new SuException("rename column: column already exists: "
						+ newname + " in " + tablename);
			Column col = table.getColumn(oldname);
			if (col == null)
				throw new SuException("rename column: nonexistent column: "
						+ oldname + " in " + tablename);

			update_any_record(t, "columns", "table,column",
					key(table.num, oldname),
					Column.record(table.num, newname, col.num));

			// update any indexes that include this column
			for (Index index : table.indexes) {
				List<String> cols = commasToList(index.columns);
				int i = cols.indexOf(oldname);
				if (i < 0)
					continue ; // this index doesn't contain the column
				cols.set(i, newname);

				String newColumns = listToCommas(cols);
				Record newRecord = t.getBtreeIndex(index).withColumns(newColumns);
				update_any_record(t, "indexes", "table,columns",
						key(table.num, index.columns), newRecord);
				}
			List<BtreeIndex> btis = new ArrayList<BtreeIndex>();
			t.updateTable(getUpdatedTable(t, table.num, btis));
			for (BtreeIndex bti : btis)
				t.btreeIndexUpdates.put(bti.tblnum + ":" + bti.columns, bti);
			t.ck_complete();
		} finally {
			t.abortIfNotComplete();
		}
	}

	// indexes ======================================================

	public void addIndex(String tablename, String columns, boolean isKey) {
		addIndex(tablename, columns, isKey, false, false, null, null, 0);
	}

	public void addIndex(String tablename, String columns,
			boolean isKey, boolean unique, boolean lower, String fktablename,
			String fkcolumns, int fkmode) {
		if (fkcolumns == null || fkcolumns.equals(""))
			fkcolumns = columns;

		Transaction tran = readwriteTran();
		try {
			Table table = tran.ck_getTable(tablename);
			ImmutableList<Integer> colnums = table.columns.nums(columns);
			if (table.hasIndex(columns))
				throw new SuException("add index: index already exists: " + columns
						+ " in " + tablename);
			BtreeIndex btreeIndex = new BtreeIndex(dest, table.num, columns, isKey,
					unique, fktablename, fkcolumns, fkmode);
			add_any_record(tran, "indexes", btreeIndex.record);
			List<BtreeIndex> btis = new ArrayList<BtreeIndex>();
			tran.updateTable(getUpdatedTable(tran, table.num, btis));
			Table fktable = null;
			if (fktablename != null) {
				fktable = tran.getTable(fktablename);
				if (fktable != null)
					tran.updateTable(getUpdatedTable(tran, fktable.num));
			}
			insertExistingRecords(tran, columns, table, colnums,
					fktablename, fktable, fkcolumns, btreeIndex);
			for (BtreeIndex bti : btis)
				if (bti.columns.equals(columns))
					tran.btreeIndexUpdates.put(bti.tblnum + ":" + bti.columns, bti);
			tran.complete();
		} catch (RuntimeException e) {
			throw e;
		} finally {
			tran.abortIfNotComplete();
		}
	}

	private void insertExistingRecords(Transaction tran, String columns,
			Table table, ImmutableList<Integer> colnums, String fktablename,
			Table fktable, String fkcolumns, BtreeIndex btreeIndex) {
		if (!table.hasIndexes())
			return;

		Index index = table.firstIndex();
		if (index == null)
			return;
		BtreeIndex.Iter iter = tran.getBtreeIndex(index).iter(tran).next();
		if (iter.eof())
			return;

		if (fktablename != null && fktable == null)
			throw new SuException("add index to " + table.name
					+ " blocked by foreign key to nonexistent table: "
					+ fktablename);

		for (; !iter.eof(); iter.next()) {
			Record rec = input(iter.keyadr());
			if (fktable != null)
				fkey_source_block1(tran, fktable, fkcolumns,
						rec.project(colnums), "add index to " + table.name);
			Record key = rec.project(colnums, iter.cur().keyadr());
			if (!btreeIndex.insert(tran, new Slot(key)))
				throw new SuException("add index: duplicate key: " + columns
						+ " = " + key + " in " + table.name);
		}
	}

	public void removeIndex(String tablename, String columns) {
		if (is_system_index(tablename, columns))
			throw new SuException("delete index: can't delete system index: "
					+ columns + " from " + tablename);
		Transaction tran = readwriteTran();
		try {
			Table table = tran.ck_getTable(tablename);
			if (table.indexes.size() == 1)
				throw new SuException("delete index: can't delete last index from "
						+ tablename);
			removeIndex(tran, table, columns);
			tran.updateTable(getUpdatedTable(tran, table.num));
			tran.ck_complete();
		} finally {
			tran.abortIfNotComplete();
		}
	}

	// used by removeIndex and removeTable
	private void removeIndex(Transaction tran, Table tbl, String columns) {
		if (!tbl.indexes.hasIndex(columns))
			throw new SuException("delete index: nonexistent index: " + columns
					+ " in " + tbl.name);

		remove_any_record(tran, "indexes", "table,columns",
				key(tbl.num, columns));
	}

	// used by schema changes
	Table getUpdatedTable(Transaction tran, int tblnum) {
		return getUpdatedTable(tran, tblnum, null);
	}

	Table getUpdatedTable(Transaction tran, int tblnum, List<BtreeIndex> btis) {
		BtreeIndex tablenum_index = tran.getBtreeIndex(TN.TABLES, "table");
		Record table_rec = find(tran, tablenum_index, key(tblnum));
		tran.tabledataUpdates.put(tblnum, new TableData(table_rec));
		return loadTable(tran, table_rec, btis);
	}

	// called by Transaction complete for schema changes
	void removeTable(Table table) {
		tables = tables.without(table);
		tabledata = tabledata.without(table.num);
		for (Index index : table.indexes)
			btreeIndexes = btreeIndexes.without(index.tblnum + ":" + index.columns);
	}

	// called by Transaction complete for schema changes
	void updateTable(Table table, TableData td) {
		tables = tables.with(table);
		tabledata = tabledata.with(td.num, td);
	}

	// called by Transaction complete for schema changes
	void updateBtreeIndex(BtreeIndex bti) {
		btreeIndexes = btreeIndexes.with(bti.tblnum + ":" + bti.columns, bti);
	}

	private Table loadTable(Transaction tran, Record table_rec,
			List<BtreeIndex> btis) {
		String tablename = table_rec.getString(Table.TABLE);
		int tblnum = table_rec.getInt(Table.TBLNUM);
		Record tblkey = key(tblnum);

		// columns
		ArrayList<Column> cols = new ArrayList<Column>();
		BtreeIndex columns_index = tran.getBtreeIndex(TN.COLUMNS, "table,column");
		for (BtreeIndex.Iter iter = columns_index.iter(tran, tblkey).next();
				!iter.eof(); iter.next())
			cols.add(new Column(input(iter.keyadr())));
		Collections.sort(cols);
		Columns columns = new Columns(ImmutableList.copyOf(cols));

		// indexes
		ImmutableList.Builder<Index> indexes = ImmutableList.builder();
		BtreeIndex indexes_index = tran.getBtreeIndex(TN.INDEXES, "table,columns");
		for (BtreeIndex.Iter iter = indexes_index.iter(tran, tblkey).next();
				!iter.eof(); iter.next()) {
			Record r = input(iter.keyadr());
			String icols = Index.getColumns(r);
			indexes.add(new Index(r, icols, columns.nums(icols),
					getForeignKeys(tran, tablename, icols)));
			if (btis != null)
				btis.add(new BtreeIndex(dest, r));
		}

		return new Table(table_rec, columns, new Indexes(indexes.build()));
	}

	// find foreign keys pointing to this index
	private List<Record> getForeignKeys(Transaction tran, String tablename,
			String columns) {
		List<Record> records = new ArrayList<Record>();
		BtreeIndex fkey_index = tran.getBtreeIndex(TN.INDEXES, "fktable,fkcolumns");
		for (BtreeIndex.Iter iter = fkey_index.iter(tran, key(tablename, columns)).next();
				!iter.eof(); iter.next())
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
		return tables.get(table).schema();
	}

	// views ========================================================

	public void add_view(String name, String definition) {
		Transaction tran = readwriteTran();
		try {
			if (null != getView(tran, name))
				throw new SuException("view: '" + name + "' already exists");
			add_any_record(tran, "views",
					new Record().add(name).add(definition));
			tran.ck_complete();
		} finally {
			tran.abortIfNotComplete();
		}
	}

	public String getView(Transaction tran, String viewname) {
		BtreeIndex views_index = tran.getBtreeIndex(TN.VIEWS, "view_name");
		Record rec = find(tran, views_index, key(viewname));
		return rec == null ? null : rec.getString(V.DEFINITION);
	}

	public void removeView(Transaction tran, String viewname) {
		remove_any_record(tran, "views", "view_name", key(viewname));
	}

	// add record ===================================================

	public void addRecord(Transaction tran, String table, Record r) {
		checkForSystemTable(table, "add record to");
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

	void add_any_record(Transaction tran, String table, Record r) {
		add_any_record(tran, tran.ck_getTable(table), r);
	}
	private void add_any_record(Transaction tran, Table table, Record rec) {
		if (tran.isReadonly())
			throw new SuException("can't output from read-only transaction to "
					+ table.name);
		assert (table != null);
		verify(!table.indexes.isEmpty());

		if (!loading)
			fkey_source_block(tran, table, rec, "add record to " + table.name);

		long adr = output(table.num, rec);
		add_index_entries(tran, table, rec, adr);
		tran.create_act(table.num, adr);

		if (!loading)
			Triggers.call(table, tran, null, rec);
	}

	void add_index_entries(Transaction tran, Table table, Record rec, long adr) {
		for (Index index : table.indexes) {
			BtreeIndex btreeIndex = tran.getBtreeIndex(index);
			Record key = rec.project(index.colnums, adr);
			// handle insert failing due to duplicate key
			if (!btreeIndex.insert(tran, new Slot(key))) {
				// delete from previous indexes
				for (Index j : table.indexes) {
					if (j == index)
						break;
					key = rec.project(j.colnums, adr);
					btreeIndex = tran.getBtreeIndex(j);
					verify(btreeIndex.remove(key));
				}
				throw new SuException("duplicate key: " + index.columns + " = "
						+ key + " in " + table.name);
			}
		}

		tran.updateTableData(tran.getTableData(table.num).with(rec.bufSize()));
	}

	// update record ================================================

	public long updateRecord(Transaction tran, long recadr, Record rec) {
		verify(recadr > 0);
		int tblnum = adr(recadr - 4).getInt(0);
		Table tbl = tran.ck_getTable(tblnum);
		checkForSystemTable(tbl.name, "update record in");
		return update_record(tran, tbl, input(recadr), rec, true);
	}

	public void updateRecord(Transaction tran, String table, String index,
			Record key, Record newrec) {
		checkForSystemTable(table, "update record in");
		update_any_record(tran, table, index, key, newrec);
	}

	void update_any_record(Transaction tran, String tablename,
			String indexcolumns, Record key, Record newrec) {
		Table table = tran.ck_getTable(tablename);
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
				fkey_source_block1(tran, tran.getTable(i.fksrc.tablename),
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
		for (Index index : table.indexes) {
			Record newkey = newrec.project(index.colnums, newoff);
			BtreeIndex btreeIndex = tran.getBtreeIndex(index);
			if (!btreeIndex.insert(tran, new Slot(newkey))) {
				// undo previous
				for (Index j : table.indexes) {
					if (j == index)
						break;
					btreeIndex = tran.getBtreeIndex(j);
					verify(btreeIndex.remove(newrec.project(j.colnums, newoff)));
				}
				tran.undo_delete_act(table.num, oldoff);
				throw new SuException("update record: duplicate key: "
						+ index.columns + " = " + newkey + " in " + table.name);
			}
		}
		tran.create_act(table.num, newoff);
		tran.updateTableData(tran.getTableData(table.num)
				.withReplace(oldrec.bufSize(), newrec.bufSize()));

		Triggers.call(table, tran, oldrec, newrec);
		return newoff;
	}

	// remove record ================================================

	public void removeRecord(Transaction tran, long recadr) {
		verify(recadr > 0);
		int tblnum = adr(recadr - 4).getInt(0);
		Table tbl = tran.ck_getTable(tblnum);
		checkForSystemTable(tbl.name, "delete record from");
		remove_any_record(tran, tbl, input(recadr));

	}

	public void removeRecord(Transaction tran, String tablename, String index,
			Record key) {
		checkForSystemTable(tablename, "delete record from");
		remove_any_record(tran, tablename, index, key);
	}

	private void remove_any_record(Transaction tran, String tablename,
			String indexcolumns, Record key) {
		Table table = tran.ck_getTable(tablename);
		// lookup key in given index
		Index index = table.indexes.get(indexcolumns);
		assert (index != null);
		Record rec = find(tran, tran.getBtreeIndex(index), key);
		if (rec == null)
			throw new SuException("delete record: can't find record in "
					+ tablename + " " + indexcolumns + " " + key);
		remove_any_record(tran, table, rec);
	}

	public void remove_any_record(Transaction tran, Table table, Record rec) {
		if (tran.isReadonly())
			throw new SuException("can't delete from read-only transaction in "
					+ table.name);
		assert (table != null);
		assert (rec != null);

		fkey_target_block(tran, table, rec, "delete from " + table.name);

		if (!tran.delete_act(table.num, rec.off()))
			throw new SuException("delete record from " + table.name
					+ " transaction conflict: " + tran.conflict());

		tran.updateTableData(tran.getTableData(table.num).without(rec.bufSize()));

		if (!loading)
			Triggers.call(table, tran, rec, null);
	}

	// foreign keys =================================================

	private void fkey_source_block(Transaction tran, Table table, Record rec, String action) {
		for (Index index : table.indexes)
			if (index.fksrc != null) {
				fkey_source_block1(tran, tran.getTable(index.fksrc.tablename),
						index.fksrc.columns, rec.project(index.colnums), action);
			}
	}

	private void fkey_source_block1(Transaction tran, Table fktbl,
			String fkcolumns, Record key, String action) {
		if (fkcolumns.equals("") || key.allEmpty())
			return;
		Index fkidx = getIndex(fktbl, fkcolumns);
		if (fkidx == null || find(tran, fkidx, key) == null)
			throw new SuException(action + " blocked by foreign key to "
					+ fktbl.name + " " + key);
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
			Table fktbl = tran.getTable(fk.tblnum);
			Index fkidx = getIndex(fktbl, fk.columns);
			if (fkidx == null)
				continue ;
			BtreeIndex.Iter iter =
					tran.getBtreeIndex(fkidx).iter(tran, key).next();
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
			BtreeIndex.Iter iter, ImmutableList<Integer> colnums) {
		Record oldrec = input(iter.keyadr());
		Record newrec = new Record();
		for (int i = 0; i < oldrec.size(); ++i) {
			int j = colnums.indexOf(i);
			if (j == -1)
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

	// =========================================================================
	// TODO remove this code once indexes not updated till commit

	// used by Transaction.abort
	void undoAdd(int tblnum, long adr) {
		Table table = tables.get(tblnum);
		if (table == null)
			return;
		Record rec = input(adr);
		remove_index_entries(table, rec);
	}

	// used by Transaction.abort
	void undoDelete(int tblnum, long adr) {
		Table table = tables.get(tblnum);
		if (table == null)
			return;
	}

	// used by Transaction.finalization
	void remove_index_entries(int tblnum, long adr) {
		Table table = tables.get(tblnum);
		if (table != null)
			remove_index_entries(table, input(adr));
	}

	private void remove_index_entries(Table table, Record rec) {
		long off = rec.off();
		for (Index index : table.indexes) {
			Record key = rec.project(index.colnums, off);
			BtreeIndex btreeIndex = getBtreeIndex(index);
			verify(btreeIndex.remove(key));
			btreeIndex.update(); // update indexes record from index
		}
	}

	private BtreeIndex getBtreeIndex(Index index) {
		return btreeIndexes.get(index.tblnum + ":" + index.columns);
	}

	// =========================================================================

	public long alloc(int n, byte type) {
		return dest.alloc(n, type);
	}

	public ByteBuffer adr(long offset) {
		return dest.adr(offset);
	}

	static byte[] bytes = new byte[256];

	private class Dbhdr {
		static final int SIZE = 4 + 4 + 4;
		private final ByteBuffer buf;
		private int next_table;
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
		public int getNextTableNum() {
			buf.putInt(0, ++next_table);
			return next_table - 1;
		}

	}

	public void setLoading(boolean loading) {
		this.loading = loading;
	}

	public long size() {
		return dest.size();
	}

	// called by Transaction complete
	public void updateTableData(int num, int nextfield, int d_nrecords,
			int d_totalsize) {
		TableData td = tabledata.get(num);
		td = td.with(nextfield, d_nrecords, d_totalsize);
		tabledata = tabledata.with(num, td);
	}

	// called by Transaction complete
	public boolean updateBtreeIndex(String key,
			BtreeIndex btiOld, BtreeIndex btiNew) {
		BtreeIndex bti = btreeIndexes.get(key);
		if (!bti.update(btiOld, btiNew))
			return false; // conflict
		btiNew.update(); // save changes to database
		btreeIndexes = btreeIndexes.with(key, btiNew);
		return true;
	}

	// called by Transaction complete
	public void addBtreeIndex(String key, BtreeIndex btiNew) {
		btreeIndexes = btreeIndexes.with(key, btiNew);
	}

}
