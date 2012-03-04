/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import suneido.immudb.DbHashTrie.Entry;
import suneido.immudb.DbHashTrie.IntEntry;
import suneido.intfc.database.DatabasePackage.Status;
import suneido.language.Triggers;
import suneido.util.FileUtils;

@ThreadSafe
class Database2 implements ImmuDatabase {
	final Transactions2 trans = new Transactions2();
	final Storage stor;
	final Storage istor;
	final ReentrantReadWriteLock exclusiveLock = new ReentrantReadWriteLock();
	private final Triggers triggers = new Triggers();
	final Object commitLock = new Object();
	/** only updated when holding commitLock */
	volatile State state;

	// create

	static Database2 create(String dbfilename) {
		FileUtils.deleteIfExisting(dbfilename);
		return create(new MmapFile(dbfilename + "d", "rw"),
				new MmapFile(dbfilename + "i", "rw"));
	}

	static Database2 create(Storage stor, Storage istor) {
		Database2 db = new Database2(stor, istor, DbHashTrie.empty(stor), new Tables(), 0);
		Bootstrap.create(db.exclusiveTran());
		Persist.persist(db);
		return db;
	}

	private Database2(Storage stor, Storage istor, DbHashTrie dbinfo, Tables schema, int cksum) {
		this.stor = stor;
		this.istor = istor;
		state = new State(dbinfo, null, cksum);
		schema = schema == null ? SchemaLoader.load(readTransaction()) : schema;
		state = new State(dbinfo, schema, cksum);
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
		// TODO check that stor matches istor
		if (! new Check(stor).fastcheck()) {
			stor.close();
			return null;
		}
		if (! new Check(istor).fastcheck()) {
			istor.close();
			return null;
		}
		return openWithoutCheck(stor, istor);
	}

	static Database2 openWithoutCheck(Storage stor, Storage istor) {
		Persist.Info info = Persist.info(istor);
		DbHashTrie dbinfo =
				DbHashTrie.load(istor, info.dbinfoadr, new DbinfoTranslator(istor));
		return new Database2(stor, istor, dbinfo, info.lastcksum);
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
		Persist.persist(this);
		return Database2.open(stor, istor);
	}

	/** used by tests */
	@Override
	public Status check() {
		Persist.persist(this);
		return DbCheck2.check(stor, istor);
	}

	private Database2(Storage stor, Storage istor, DbHashTrie dbinfo, int cksum) {
		this(stor, istor, dbinfo, null, cksum);
	}

	// used by DbCheck
	Tables schema() {
		return state.schema;
	}

	@Override
	public ReadTransaction2 readTransaction() {
		int num = trans.nextNum(true);
		return new ReadTransaction2(num, this);
	}

	@Override
	public UpdateTransaction2 updateTransaction() {
		int num = trans.nextNum(false);
		return new UpdateTransaction2(num, this);
	}

	@Override
	public ExclusiveTransaction2 exclusiveTran() {
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
			: TableBuilder.alter(readTransaction(), tableName);
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

	String getView(String name) {
		ReadTransaction2 t = readTransaction();
		try {
			return Views.getView(t, name);
		} finally {
			t.complete();
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
		Persist.persist(this);
		stor.close();
		istor.close();
	}

	@Override
	public long size() {
		return stor.sizeFrom(0);
	}

	@Override
	public String getSchema(String tableName) {
		ReadTransaction2 t = readTransaction();
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
			ReadTransaction2 t, Table table, Record oldrec, Record newrec) {
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

	/** called by transaction commit and by persist */
	void setState(DbHashTrie dbinfo, Tables schema, int cksum) {
		this.state = new State(dbinfo, schema, cksum);
	}

	@Immutable
	static class State {
		final DbHashTrie dbinfo;
		final Tables schema;
		/** checksum of last commit */
		final int lastcksum;

		private State(DbHashTrie dbinfo, Tables schema, int lastcksum) {
			assert dbinfo.immutable();
			this.dbinfo = dbinfo;
			this.schema = schema;
			this.lastcksum = lastcksum;
		}
	}

}
