/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.io.File;
import java.util.List;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

import suneido.HttpServerMonitor;
import suneido.SuException;
import suneido.immudb.DbHashTrie.Entry;
import suneido.immudb.DbHashTrie.IntEntry;
import suneido.intfc.database.DatabasePackage.Status;
import suneido.intfc.database.DatabasePackage.StringObserver;
import suneido.runtime.Triggers;
import suneido.util.Errlog;
import suneido.util.FileUtils;

@ThreadSafe
class Database implements suneido.intfc.database.Database, AutoCloseable {
	final Transactions trans = new Transactions();
	final String filename;
	final Storage dstor;
	final Storage istor;
	private final Triggers triggers = new Triggers();
	final Object commitLock = new Object();
	/** only updated when holding commitLock */
	volatile State state;
	private State lastPersistState;
	private boolean corrupt = false;
	private static enum Ck { CHECK, NOCHECK };
	private volatile boolean closed = false;

	// create

	static Database create(String dbfilename) {
		FileUtils.deleteIfExisting(dbfilename + "d");
		FileUtils.deleteIfExisting(dbfilename + "i");
		return create(dbfilename,
				new MmapFile(dbfilename + "d", "rw"),
				new MmapFile(dbfilename + "i", "rw"));
	}

	static Database create(String filename, Storage dstor, Storage istor) {
		Database db = new Database(filename, dstor, istor,
				DbHashTrie.empty(istor), new Tables());
		Bootstrap.create(db.schemaTransaction());
		db.persist();
		return db;
	}

	private Database(String filename, Storage dstor, Storage istor,
			DbHashTrie dbinfo, Tables schema) {
		this.filename = filename;
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
		return open(filename, mode, Ck.CHECK);
	}

	static Database open(String filename, String mode, Ck ck) {
		// prevent empty files from being created
		if (! new File(filename + "d").exists() ||
				! new File(filename + "i").exists())
			throw new SuException("missing database files");
		try {
			return open(filename, ck,
					new MmapFile(filename + "d", mode),
					new MmapFile(filename + "i", mode));
		} catch (Throwable e) {
			throw new SuException("error opening database", e);
		}
	}

	static Database open(String filename, Storage dstor, Storage istor) {
		return open(filename, Ck.CHECK, dstor, istor);
	}

	static Database open(String filename, Ck ck, Storage dstor, Storage istor) {
		if (ck == Ck.CHECK) {
			if (dstor.sizeFrom(0) == 0 || istor.sizeFrom(0) == 0)
				throw new SuException("invalid empty database file");
			if (! openCheck(filename, dstor, istor)) {
				dstor.close();
				istor.close();
				return null;
			}
		}
		return openWithoutCheck(filename, dstor, istor);
	}

	private static boolean openCheck(String filename, Storage dstor, Storage istor) {
		return filename.equals("") ||
				DbGood.check(filename + "c", dstor.sizeFrom(0))
					? new Check(dstor, istor).fastcheck()
					: fullCheck(dstor, istor);
	}

	private static boolean fullCheck(Storage dstor, Storage istor) {
		Errlog.warn("full check required - database not shut down properly?");
		HttpServerMonitor.checking();
		boolean ok = new Check(dstor, istor).fullcheck();
		//BUG: if check fails, then rebuild will do another redundant check
		HttpServerMonitor.starting();
		return ok;
	}

	static Database openWithoutCheck(String filename) {
		return open(filename, "rw", Ck.NOCHECK);
	}

	static Database openWithoutCheck(String filename, Storage dstor, Storage istor) {
		return new Database(filename, dstor, istor);
	}

	private Database(String filename, Storage dstor, Storage istor) {
		this.filename = filename;
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
		return Database.open(filename, dstor, istor);
	}

	@Override
	public String check() {
		int dUpTo, iUpTo;
		synchronized (commitLock) {
			persist();
			dUpTo = dstor.upTo();
			iUpTo = istor.upTo();
		}
		StringObserver so = new StringObserver();
		Status status = DbCheck.check(filename, dstor, istor, dUpTo, iUpTo, so);
		if (status != Status.OK) {
			HttpServerMonitor.corrupt();
			corrupt = true; // prevent writing dbc file
			trans.lock(); // silently abort all transactions from now on
		}
		return status == Status.OK ? "" : so.toString();
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
		if (closed)
			return;
		closed = true;
		long size;
		synchronized (commitLock) {
			persist();
			size = dstor.sizeFrom(0);
			dstor.close();
			istor.close();
		}
		if (! corrupt && ! filename.equals(""))
			DbGood.create(filename + "c", size);
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
		if (trans.isLocked())
			return ImmutableList.of(Integer.valueOf(0));
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
