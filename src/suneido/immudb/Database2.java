/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import suneido.immudb.DbHashTrie.Entry;
import suneido.immudb.DbHashTrie.IntEntry;
import suneido.intfc.database.DatabasePackage.Status;
import suneido.language.Triggers;
import suneido.util.FileUtils;

import com.google.common.base.Objects;

@ThreadSafe
class Database2 implements ImmuDatabase {
	private static final int PERSIST_EVERY = 1000;
	final Transactions2 trans = new Transactions2();
	final Storage dstor;
	final Storage istor;
	final ReentrantReadWriteLock exclusiveLock = new ReentrantReadWriteLock();
	private final Triggers triggers = new Triggers();
	final Object commitLock = new Object();
	/** only updated when holding commitLock */
	volatile State state;
	State lastPersistState;
	AtomicInteger nUpdateTran = new AtomicInteger();

	// create

	static Database2 create(String dbfilename) {
		FileUtils.deleteIfExisting(dbfilename + "d");
		FileUtils.deleteIfExisting(dbfilename + "i");
		return create(new MmapFile(dbfilename + "d", "rw"),
				new MmapFile(dbfilename + "i", "rw"));
	}

	static Database2 create(Storage dstor, Storage istor) {
		Database2 db = new Database2(dstor, istor, DbHashTrie.empty(istor), new Tables());
		Bootstrap.create(db.exclusiveTran());
		db.persist();
		return db;
	}

	private Database2(Storage dstor, Storage istor, DbHashTrie dbinfo, Tables schema) {
		this.dstor = dstor;
		this.istor = istor;
		state = new State(dbinfo, null, 0, 0);
		schema = schema == null ? SchemaLoader.load(readTransaction()) : schema;
		state = lastPersistState = new State(dbinfo, schema, 0, 0);
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

	static Database2 open(Storage dstor, Storage istor) {
		if (! new Check2(dstor, istor).fastcheck()) {
			dstor.close();
			istor.close();
			return null;
		}
		return openWithoutCheck(dstor, istor);
	}

	static Database2 openWithoutCheck(Storage dstor, Storage istor) {
		DbHashTrie dbinfo = DbHashTrie.load(istor,
				Persist.dbinfoadr(istor), new DbinfoLoader(istor));
		return new Database2(dstor, istor, dbinfo);
	}

	static class DbinfoLoader implements DbHashTrie.Translator {
		final Storage dstor;

		DbinfoLoader(Storage dstor) {
			this.dstor = dstor;
		}

		@Override
		public Entry translate(Entry e) {
			if (e instanceof IntEntry) {
				int adr = ((IntEntry) e).value;
				Record rec = Record.from(dstor, adr);
				return new TableInfo(rec, adr);
			} else
				throw new RuntimeException("DbinfoTranslator bad type " + e);
		}
	}

	/** reopens with same Storage */
	@Override
	public Database2 reopen() {
		persist();
		return Database2.open(dstor, istor);
	}

	/** used by tests */
	@Override
	public Status check() {
		persist();
		return DbCheck2.check(dstor, istor);
				//suneido.intfc.database.DatabasePackage.printObserver);
	}

	private Database2(Storage dstor, Storage istor, DbHashTrie dbinfo) {
		this(dstor, istor, dbinfo, null);
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
		// MAYBE do this inside UpdateTransaction commit
		// then you don't need Atomic
		// and you already have commit lock
		if (nUpdateTran.incrementAndGet() >= PERSIST_EVERY) {
			nUpdateTran.set(0);
			persist();
		}
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
		persist();
		dstor.close();
		istor.close();
	}

	void persist() {
		if (state == lastPersistState)
			return;
		Persist.persist(this);
		lastPersistState = state;
	}

	@Override
	public long size() {
		return dstor.sizeFrom(0);
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
	void setState(DbHashTrie dbinfo, Tables schema, int cksum, int adr) {
		assert adr != 0;
		this.state = new State(dbinfo, schema, cksum, adr);
	}

	@Immutable
	static class State {
		final DbHashTrie dbinfo;
		final Tables schema;
		/** checksum of last data commit */
		final int lastcksum;
		/** address of last data commit */
		final int lastadr;

		private State(DbHashTrie dbinfo, Tables schema,
				int lastcksum, int lastadr) {
			assert dbinfo.immutable();
			this.dbinfo = dbinfo;
			this.schema = schema;
			this.lastcksum = lastcksum;
			this.lastadr = lastadr;
		}

		@Override
		public String toString() {
			return Objects.toStringHelper(this)
					.add("lastadr", lastadr)
					.add("lastcksum", lastcksum)
					.toString();
		}
	}

	void dump() {
		Dump2.dump(dstor, istor);
	}

}
