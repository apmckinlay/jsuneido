package suneido.database;

import static suneido.SuException.verify;
import static suneido.Suneido.errlog;
import static suneido.database.Transaction.NULLTRAN;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.*;

import javax.annotation.concurrent.ThreadSafe;

import suneido.SuException;
import suneido.util.*;

import com.google.common.collect.ImmutableList;

/**
 * Implements the Suneido database. Uses {@link Mmfile} and {@link BtreeIndex}.
 * See also {@link Schema} and {@link Data}
 * Transactions handled by {@link Transaction} and {@link Transactions}.
 *
 * @author Andrew McKinlay
 * <p><small>Copyright 2008 Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.</small></p>
 */
@ThreadSafe
public class Database {
//	private final File file;
	private final Mode mode;
	public final Destination dest; // used by tests
	private Dbhdr dbhdr;
	private final Checksum checksum = new Checksum();
	public boolean loading = false;
	private byte output_type = Mmfile.DATA;
	private volatile Tables tables = new Tables(); // depends on Tables being immutable
	private volatile PersistentMap<Integer, TableData> tabledata = null;
	private volatile PersistentMap<String, BtreeIndex> btreeIndexes =
			PersistentMap.empty();
	private final Transactions trans = new Transactions(this);
	public final Object commitLock = new Object();
	public static Database theDB;

	public static class TN {
		public final static int TABLES = 1, COLUMNS = 2, INDEXES = 3, VIEWS = 4;
	}
	private static class V {
		@SuppressWarnings("unused")
		final static int NAME = 0, DEFINITION = 1;
	}
	private final static int VERSION = 1;

	public static void open_theDB() {
		theDB = new Database("suneido.db", Mode.OPEN);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				theDB.close();
				theDB = null;
			}
		});
	}

	public Database(String filename, Mode mode) {
		this(new File(filename), mode);
	}

	public Database(File file, Mode mode) {
//		this.file = file;
		this.mode = mode;
		dest = new Mmfile(file, mode);
		init(mode);
	}

	public Database(Destination dest, Mode mode) {
//		this.file = null;
		this.mode = mode;
		this.dest = dest;
		init(mode);
	}

	private void init(Mode mode) {
		if (mode == Mode.OPEN && ! Session.check_shutdown(dest)) {
			errlog("database not shut down properly last time");
//			if (file == null)
				throw new SuException("database not shut down properly last time");
//			DbRebuild.rebuildOrExit(file.getPath());
		}
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
		if (mode != Mode.READ_ONLY)
			Session.startup(dest);
		dest.force();
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

	// only used by DatabaseTest
	int getNrecords(String tablename) {
		return tabledata.get(tables.get(tablename).num).nrecords;
	}

	synchronized long output(int tblnum, Record r) {
		int n = r.packSize();
		long offset = dest.alloc(4 + n, output_type);
		ByteBuffer p = dest.adr(offset).getByteBuffer();
		p.putInt(tblnum);
		r.pack(p);
		// don't checksum tables or indexes records because they get updated
		if (output_type == Mmfile.DATA
				&& tblnum != TN.TABLES && tblnum != TN.INDEXES)
			checksum.add(p, 4 + n);
		return offset + 4; // offset of record i.e. past tblnum
	}

	void writeCommit(ByteBuffer buf) {
		// include commit in checksum, but don't include checksum itself
		synchronized(checksum) {
			checksum.add(buf, buf.position());
			buf.putInt((int) checksum.getValue());
			checksum.reset();
		}
	}

	// not synchronized because it does not depend on state
	// only uses dest
	public Record input(long adr) {
		verify(adr != 0);
		return new Record(dest.adr(adr), adr);
	}

	static Record key(int tblnum, String columns) {
		return new Record().add(tblnum).add(columns);
	}

	static Record key(int i) {
		return new Record().add(i);
	}

	Record find(Transaction tran, BtreeIndex btreeIndex, Record key) {
		Slot slot = btreeIndex.find(tran, key);
		return slot == null ? null : input(slot.keyadr());
	}

	public Record getTableRecord(Transaction tran, int tblnum) {
		BtreeIndex tablenum_index = tran.getBtreeIndex(TN.TABLES, "table");
		return find(tran, tablenum_index, key(tblnum));
	}

	public void close() {
		if (mode != Mode.READ_ONLY)
			Session.shutdown(dest);
		dest.close();
	}

	public Transaction readonlyTran() {
		synchronized(commitLock) {
			return new Transaction(trans, true, tables, tabledata, btreeIndexes);
		}
	}
	public Transaction readwriteTran() {
		synchronized(commitLock) {
			return new Transaction(trans, false, tables, tabledata, btreeIndexes);
		}
	}

	public Table loadTable(Transaction tran, Record table_rec,
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
				btis.add(new BtreeIndex(dest, r)); // TODO check use of dest
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

	// views ========================================================

	public void add_view(String name, String definition) {
		Transaction tran = readwriteTran();
		try {
			if (null != getView(tran, name))
				throw new SuException("view: '" + name + "' already exists");
			Data.add_any_record(tran, "views",
					new Record().add(name).add(definition));
			tran.ck_complete();
		} finally {
			tran.abortIfNotComplete();
		}
	}

	public static String getView(Transaction tran, String viewname) {
		BtreeIndex views_index = tran.getBtreeIndex(TN.VIEWS, "view_name");
		Record rec = tran.db.find(tran, views_index, key(viewname));
		return rec == null ? null : rec.getString(V.DEFINITION);
	}

	public static void removeView(Transaction tran, String viewname) {
		Data.remove_any_record(tran, "views", "view_name", key(viewname));
	}

	private static Record key(String s) {
		return new Record().add(s);
	}

	public long alloc(int n, byte type) {
		return dest.alloc(n, type);
	}

	public ByteBuf adr(long offset) {
		return dest.adr(offset);
	}

	private class Dbhdr {
		static final int SIZE = 4 + 4 + 4;
		private final ByteBuffer buf;
		private int next_table;
		final long indexes;

		// create
		Dbhdr(long at, long indexes_adr) {
			verify(at == dest.first());
			buf = adr(at).getByteBuffer();
			buf.putInt(next_table = TN.INDEXES + 1);
			buf.putInt(Mmfile.offsetToInt(indexes = indexes_adr));
			buf.putInt(VERSION);
		}

		// open
		Dbhdr() {
			long at = dest.first();
			if (dest.length(at) < SIZE)
				throw new SuException("invalid database");
			buf = adr(dest.first()).getByteBuffer();
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

		public void setNextTableNum(int nextTableNum) {
			buf.putInt(0, nextTableNum);
		}

	}

	int getNextTableNum() {
		return dbhdr.getNextTableNum();
	}

	// for rebuild
	public void setNextTableNum(int nextTableNum) {
		dbhdr.setNextTableNum(nextTableNum);
	}

	public void setLoading(boolean loading) {
		this.loading = loading;
	}

	public long size() {
		return dest.size();
	}

	// not synchronized since only called by Transaction complete
	// which is single threaded

	// called by Transaction complete for schema changes
	void removeTableCommit(Table table) {
		tables = tables.without(table);
		tabledata = tabledata.without(table.num);
		for (Index index : table.indexes)
			btreeIndexes = btreeIndexes.without(index.tblnum + ":" + index.columns);
	}

	// called by Transaction complete for schema changes
	// and by DbRebuild
	public void updateTable(Table table, TableData td) {
		tables = tables.with(table);
		tabledata = tabledata.with(td.tblnum, td);
	}

	// called by Transaction complete for schema changes
	// and by DbRebuild
	public void updateBtreeIndex(BtreeIndex bti) {
		bti.setDest(bti.getDest().unwrap());
		btreeIndexes = btreeIndexes.with(bti.tblnum + ":" + bti.columns, bti);
	}

	void updateTableData(int tblnum, int nextfield, int d_nrecords,
			int d_totalsize) {
		TableData td = tabledata.get(tblnum);
		td = td.with(nextfield, d_nrecords, d_totalsize);
		tabledata = tabledata.with(tblnum, td);
	}

	void addBtreeIndex(String key, BtreeIndex btiNew) {
		btiNew.setDest(btiNew.getDest().unwrap());
		btreeIndexes = btreeIndexes.with(key, btiNew);
	}

	void updateBtreeIndex(String key,
			BtreeIndex btiOld, BtreeIndex btiNew) {
		BtreeIndex bti = btreeIndexes.get(key);
		verify(bti.update(btiOld, btiNew));
		btiNew.update(); // save changes to database
		btiNew.setDest(btiNew.getDest().unwrap());
		btreeIndexes = btreeIndexes.with(key, btiNew);
	}

	// delegate

	public void addTable(String tablename) {
		Schema.addTable(this, tablename);
	}

	public boolean ensureTable(String tablename) {
		return Schema.ensureTable(this, tablename);
	}

	public void addColumn(String tablename, String column) {
		Schema.addColumn(this, tablename, column);
	}

	public void ensureColumn(String tablename, String column) {
		Schema.ensureColumn(this, tablename, column);
	}

	public void addIndex(String tablename, String columns, boolean isKey) {
		addIndex(tablename, columns, isKey, false, false, null, null, 0);	}

	public void addIndex(String tablename, String columns, boolean isKey,
			boolean unique, boolean lower,
			String fktablename, String fkcolumns, int fkmode) {
		Schema.addIndex(this, tablename, columns, isKey, unique, lower,
				fktablename, fkcolumns, fkmode);
	}

	public void ensureIndex(String tablename, String columns, boolean isKey,
			boolean unique, boolean lower,
			String fktablename, String fkcolumns, int fkmode) {
		Schema.ensureIndex(this, tablename, columns, isKey, unique, lower,
				fktablename, fkcolumns, fkmode);
	}

	public void renameTable(String oldname, String newname) {
		Schema.renameTable(this, oldname, newname);
	}

	public void renameColumn(String tablename, String oldname, String newname) {
		Schema.renameColumn(this, tablename, oldname, newname);
	}

	public boolean removeTable(String tablename) {
		return Schema.removeTable(this, tablename);
	}

	public void removeColumn(String tablename, String column) {
		Schema.removeColumn(this, tablename, column);
	}

	public void removeIndex(String tablename, String columns) {
		Schema.removeIndex(this, tablename, columns);
	}

	// used by tests
	public void checkTransEmpty() {
		trans.checkTransEmpty();
	}

	// used by tests
	public Table getTable(String tablename) {
		return tables.get(tablename);
	}

	public void addIndexEntriesForRebuild(int tblnum, Record rec) {
		Table table = tables.get(tblnum);
		for (Index index : table.indexes) {
			BtreeIndex btreeIndex = btreeIndexes.get(table.num + ":" + index.columns);
			Record key = rec.project(index.colnums, rec.off());
			if (!btreeIndex.insert(NULLTRAN, new Slot(key)))
				throw new SuException("duplicate key: " + index.columns + " = "
						+ key + " in " + table.name);
			btreeIndex.update(); // PERF only update if changed
		}
		TableData td = tabledata.get(tblnum);
		td = td.with(td.nextfield, 1, rec.packSize());
		tabledata = tabledata.with(tblnum, td);
	}

	public void removeIndexEntriesForRebuild(int tblnum, Record rec) {
		Table table = tables.get(tblnum);
		for (Index index : table.indexes) {
			BtreeIndex btreeIndex = btreeIndexes.get(table.num + ":" + index.columns);
			Record key = rec.project(index.colnums, rec.off());
			verify(btreeIndex.remove(key));
		}
		TableData td = tabledata.get(tblnum);
		td = td.without(rec.packSize());
		tabledata = tabledata.with(tblnum, td);
	}

	public void addIndexEntriesForCompact(Table table, Index index, Record rec) {
		BtreeIndex btreeIndex = btreeIndexes.get(table.num + ":" + index.columns);
		Record key = rec.project(index.colnums, rec.off());
		if (!btreeIndex.insert(NULLTRAN, new Slot(key)))
			throw new SuException("duplicate key: " + index.columns + " = "
					+ key + " in " + table.name);
		btreeIndex.update(); // PERF only update if changed
	}

	public List<Integer> tranlist() {
		return trans.tranlist();
	}

	public void limitOutstandingTransactions() {
		trans.limitOutstanding();
	}

	public int finalSize() {
		return trans.finalSize();
	}

}
