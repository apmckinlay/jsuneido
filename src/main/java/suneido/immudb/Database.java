/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.io.File;
import java.util.List;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import suneido.SuException;
import suneido.immudb.DbHashTrie.Entry;
import suneido.immudb.DbHashTrie.IntEntry;
import suneido.intfc.database.DatabasePackage.Status;
import suneido.runtime.Triggers;
import suneido.util.FileUtils;

import com.google.common.base.MoreObjects;

@ThreadSafe
class Database implements suneido.intfc.database.Database {
	final Transactions trans = new Transactions();
	final Storage dstor;
	final Storage istor;
	private final Triggers triggers = new Triggers();
	final Object commitLock = new Object();
	/** only updated when holding commitLock */
	volatile State state;
	private State lastPersistState;

	// create

	static Database create(String dbfilename) {
		FileUtils.deleteIfExisting(dbfilename + "d");
		FileUtils.deleteIfExisting(dbfilename + "i");
		return create(new MmapFile(dbfilename + "d", "rw"),
				new MmapFile(dbfilename + "i", "rw"));
	}

	static Database create(Storage dstor, Storage istor) {
		Database db = new Database(dstor, istor, DbHashTrie.empty(istor), new Tables());
		Bootstrap.create(db.schemaTransaction());
		db.persist();
		return db;
	}

	private Database(Storage dstor, Storage istor, DbHashTrie dbinfo, Tables schema) {
		this.dstor = dstor;
		this.istor = istor;
		state = lastPersistState = new State(0, dbinfo, schema, 0, 0);
	}

	// open

	/** @return null if fast check fails */
	static Database open(String filename) {
		return open(filename, "rw");
	}

	static Database openReadonly(String filename) {
		return open(filename, "r");
	}

	static Database open(String filename, String mode) {
		// prevent empty files from being created
		if (! new File(filename + "d").exists() ||
				! new File(filename + "i").exists())
			throw new SuException("missing database files");
		try {
			return open(new MmapFile(filename + "d", mode),
					new MmapFile(filename + "i", mode));
		} catch (Throwable e) {
			throw new SuException("error opening database", e);
		}
	}

	static Database open(Storage dstor, Storage istor) {
		if (dstor.sizeFrom(0) == 0 || istor.sizeFrom(0) == 0)
			throw new SuException("invalid empty database file");
		if (! new Check(dstor, istor).fastcheck()) {
			dstor.close();
			istor.close();
			return null;
		}
		return openWithoutCheck(dstor, istor);
	}

	static Database openWithoutCheck(Storage dstor, Storage istor) {
		return new Database(dstor, istor);
	}

	private Database(Storage dstor, Storage istor) {
		this.dstor = dstor;
		this.istor = istor;
		int dbinfoadr = Persist.dbinfoadr(istor);
		int maxTblnum = Persist.maxTblnum(istor);
		DbHashTrie dbinfo = DbHashTrie.load(istor, dbinfoadr, new DbinfoLoader(istor));
		state = new State(0, dbinfo, null, 0, 0); // enough to load schema
		Tables schema = SchemaLoader.load(readTransaction(), maxTblnum);
		state = lastPersistState = new State(dbinfoadr, dbinfo, schema, 0, 0);
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
	public Database reopen() {
		persist();
		return Database.open(dstor, istor);
	}

	/** used by tests */
	@Override
	public Status check() {
		persist();
		return DbCheck.check(dstor, istor);
				//suneido.intfc.database.DatabasePackage.printObserver);
	}

	void dump(boolean detail) {
		Dump.dump(dstor, istor, detail);
	}

	// used by DbCheck
	Tables schema() {
		return state.schema;
	}

	@Override
	public ReadTransaction readTransaction() {
		int num = trans.nextNum(true);
		return new ReadTransaction(num, this);
	}

	@Override
	public UpdateTransaction updateTransaction() {
		int num = trans.nextNum(false);
		return new UpdateTransaction(num, this);
	}

	void persist() {
		if (state == lastPersistState)
			return;
		Persist.persist(this);
	}

	// called by Persist when it's finished
	void setPersistState() {
		lastPersistState = state;
	}

	SchemaTransaction schemaTransaction() {
		int num = trans.nextNum(false);
		return new SchemaTransaction(num, this);
	}

	BulkTransaction bulkTransaction() {
		persist();
		int num = trans.nextNum(false);
		return new BulkTransaction(num, this);
	}

	// schema updates ----------------------------------------------------------

	@Override
	public TableBuilder createTable(String tableName) {
		checkForSystemTable(tableName, "create");
		return TableBuilder.create(schemaTransaction(), tableName, nextTableNum());
	}

	int nextTableNum() {
		return state.schema.maxTblnum + 1;
	}

	@Override
	public TableBuilder alterTable(String tableName) {
		checkForSystemTable(tableName, "alter");
		return TableBuilder.alter(schemaTransaction(), tableName);
	}

	@Override
	public TableBuilder ensureTable(String tableName) {
		checkForSystemTable(tableName, "ensure");
		return TableBuilder.ensure(schemaTransaction(), tableName, nextTableNum());
	}

	@Override
	public boolean dropTable(String tableName) {
		checkForSystemTable(tableName, "drop");
		SchemaTransaction t = schemaTransaction();
		try {
			return TableBuilder.dropTable(t, tableName);
		} finally {
			t.abortIfNotComplete();
		}
	}

	@Override
	public void renameTable(String from, String to) {
		checkForSystemTable(from, "rename");
		SchemaTransaction t = schemaTransaction();
		try {
			TableBuilder.renameTable(t, from, to);
		} finally {
			t.abortIfNotComplete();
		}
	}

	@Override
	public void addView(String name, String definition) {
		checkForSystemTable(name, "create view");
		// use SchemaTransaction to allow modifying system tables
		SchemaTransaction t = schemaTransaction();
		try {
			if (null != Views.getView(t, name))
				throw new RuntimeException("view: '" + name + "' already exists");
			Views.addView(t, name, definition);
			t.ck_complete();
		} finally {
			t.abortIfNotComplete();
		}
	}

	//--------------------------------------------------------------------------

	String getView(String name) {
		ReadTransaction t = readTransaction();
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

	@Override
	public long size() {
		return dstor.sizeFrom(0) + istor.sizeFrom(0);
	}

	@Override
	public String getSchema(String tableName) {
		ReadTransaction t = readTransaction();
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
		//System.out.println("PERSIST");
		dstor.force();
		persist();
		istor.force();
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

	@Override
	public void checkTransEmpty() {
		trans.checkTransEmpty();
	}

	/** called by transaction commit and by persist */
	void setState(int dbinfoadr, DbHashTrie dbinfo, Tables schema, int lastcksum, int lastadr) {
		assert lastadr != 0;
		this.state = new State(dbinfoadr, dbinfo, schema, lastcksum, lastadr);
	}

	@Immutable
	static class State {
		final DbHashTrie dbinfo;
		final int dbinfoadr;
		final Tables schema;
		/** checksum of last data commit */
		final int lastcksum;
		/** address of last data commit */
		final int lastadr;

		private State(int dbinfoadr, DbHashTrie dbinfo, Tables schema,
				int lastcksum, int lastadr) {
			assert dbinfo.immutable();
			this.dbinfoadr = dbinfoadr;
			this.dbinfo = dbinfo;
			this.schema = schema;
			this.lastcksum = lastcksum;
			this.lastadr = lastadr;
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.add("dbinfoadr", dbinfoadr)
					.add("lastadr", lastadr)
					.add("lastcksum", lastcksum)
					.toString();
		}
	}

}
