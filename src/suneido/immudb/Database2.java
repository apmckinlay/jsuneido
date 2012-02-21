/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.concurrent.ThreadSafe;

import suneido.immudb.DbHashTrie.Entry;
import suneido.immudb.DbHashTrie.IntEntry;
import suneido.intfc.database.DatabasePackage.Status;
import suneido.language.Triggers;
import suneido.util.FileUtils;

@ThreadSafe
class Database2 implements suneido.intfc.database.Database {
	private static final int INT_SIZE = 4;
	final Transactions2 trans = new Transactions2();
	final Storage stor;
	final Storage istor;
	final ReentrantReadWriteLock exclusiveLock = new ReentrantReadWriteLock();
	private final Triggers triggers = new Triggers();
	final Object commitLock = new Object();
	/** only updated when holding commitLock */
	volatile DatabaseState2 state;

	// create

	static Database2 create(String dbfilename) {
		FileUtils.deleteIfExisting(dbfilename);
		return create(new MmapFile(dbfilename + "d", "rw"),
				new MmapFile(dbfilename + "i", "rw"));
	}

	static Database2 create(Storage stor, Storage istor) {
		Database2 db = new Database2(stor, istor, DbHashTrie.empty(stor), new Tables());
		Bootstrap.create(db.exclusiveTran());
		return db;
	}

	private Database2(Storage stor, Storage istor, DbHashTrie dbinfo, Tables schema) {
		this.stor = stor;
		this.istor = istor;
		state = new DatabaseState2(dbinfo, null);
		schema = schema == null ? SchemaLoader.load(readonlyTran()) : schema;
		state = new DatabaseState2(dbinfo, schema);
	}

	// open

	static Database2 open(String filename) {
		return open(new MmapFile(filename + "d", "rw"),
				new MmapFile(filename + "i", "rw"));
	}

	static Database2 openReadonly(String filename) {
		return open(new MmapFile(filename + "d", "r"),
				new MmapFile(filename + "i", "r"));
	}

	static Database2 open(Storage stor, Storage istor) {
		Check check = new Check(stor);
		if (! check.fastcheck()) {
			stor.close();
			return null;
		}
		return openWithoutCheck(stor, istor);
	}

	static Database2 openWithoutCheck(Storage stor, Storage istor) {
		ByteBuffer buf = stor.buffer(-(Tran.TAIL_SIZE + 2 * INT_SIZE));
		int adr = buf.getInt();
		DbHashTrie dbinfo = DbHashTrie.load(stor, adr, new DbinfoTranslator(stor));
		return new Database2(stor, istor, dbinfo);
	}

	static class DbinfoTranslator implements DbHashTrie.Translator {
		final Storage stor;

		DbinfoTranslator(Storage stor) {
			this.stor = stor;
		}

		@Override
		public Entry translate(Entry e) {
			if (e instanceof IntEntry) {
				int adr = ((IntEntry) e).value;
				Record rec = Record.from(stor, adr);
				return new TableInfo(rec, adr);
			} else
				throw new RuntimeException("DbinfoTranslator bad type " + e);
		}
	}

	/** reopens with same Storage */
	@Override
	public Database2 reopen() {
		return Database2.open(stor, istor);
	}

	/** used by tests */
	Status check() {
		return DbCheck.check(stor);
	}

	private Database2(Storage stor, Storage istor, DbHashTrie dbinfo) {
		this(stor, istor, dbinfo, null);
	}

	// used by DbCheck
	Tables schema() {
		return state.schema;
	}

	@Override
	public ReadTransaction2 readonlyTran() {
		int num = trans.nextNum(true);
		return new ReadTransaction2(num, this);
	}

	@Override
	public UpdateTransaction2 readwriteTran() {
		int num = trans.nextNum(false);
		return new UpdateTransaction2(num, this);
	}

	ExclusiveTransaction2 exclusiveTran() {
		int num = trans.nextNum(false);
		return new ExclusiveTransaction2(num, this);
	}

	@Override
	public TableBuilder createTable(String tableName) {
		checkForSystemTable(tableName, "create");
		return TableBuilder.create(exclusiveTran(), tableName, nextTableNum());
	}

	int nextTableNum() {
		return state.schema.maxTblNum + 1;
	}

	@Override
	public TableBuilder alterTable(String tableName) {
		checkForSystemTable(tableName, "alter");
		return TableBuilder.alter(exclusiveTran(), tableName);
	}

	@Override
	public TableBuilder ensureTable(String tableName) {
		checkForSystemTable(tableName, "ensure");
		return state.schema.get(tableName) == null
			? TableBuilder.create(exclusiveTran(), tableName, nextTableNum())
			: TableBuilder.alter(readonlyTran(), tableName);
	}

	@Override
	public boolean dropTable(String tableName) {
		checkForSystemTable(tableName, "drop");
		ExclusiveTransaction2 t = exclusiveTran();
		try {
			return TableBuilder.dropTable(t, tableName);
		} finally {
			t.abortIfNotComplete();
		}
	}

	@Override
	public void renameTable(String from, String to) {
		checkForSystemTable(from, "rename");
		ExclusiveTransaction2 t = exclusiveTran();
		try {
			TableBuilder.renameTable(t, from, to);
		} finally {
			t.abortIfNotComplete();
		}
	}

	@Override
	public void addView(String name, String definition) {
		checkForSystemTable(name, "create view");
		ExclusiveTransaction2 t = exclusiveTran();
		try {
			if (null != Views.getView(t, name))
				throw new RuntimeException("view: '" + name + "' already exists");
			Views.addView(t, name, definition);
			t.complete();
		} finally {
			t.abortIfNotComplete();
		}
	}

	static void checkForSystemTable(String tablename, String operation) {
		if (isSystemTable(tablename))
			throw new RuntimeException("can't " + operation +
					" system table: " + tablename);
	}

	static boolean isSystemTable(String table) {
		return table.equals("tables") || table.equals("columns")
				|| table.equals("indexes") || table.equals("views");
	}

	@Override
	public void close() {
		stor.close();
	}

	@Override
	public long size() {
		return stor.sizeFrom(0);
	}

	@Override
	public String getSchema(String tableName) {
		ReadTransaction2 t = readonlyTran();
		try {
			Table tbl = t.getTable(tableName);
			return tbl == null ? null : tbl.schema();
		} finally {
			t.complete();
		}
	}

	@Override
	public List<Integer> tranlist() {
		return trans.tranlist();
	}

	@Override
	public void limitOutstandingTransactions() {
		trans.limitOutstanding();
	}

	@Override
	public int finalSize() {
		return trans.finalSize();
	}

	@Override
	public void force() {
	}

	@Override
	public void disableTrigger(String table) {
		triggers.disableTrigger(table);
	}

	@Override
	public void enableTrigger(String table) {
		triggers.enableTrigger(table);
	}

	void callTrigger(
			ReadTransaction t, Table table, Record oldrec, Record newrec) {
		triggers.call(t, table, oldrec, newrec);
	}

	void checkLock() {
		if (exclusiveLock.isWriteLocked())
			throw new RuntimeException("should not be locked");
	}

	@Override
	public void checkTransEmpty() {
		trans.checkTransEmpty();
	}

	/** called by transaction commit */
	void setState(DatabaseState2 state) {
		this.state = state;
	}

}
