/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import static suneido.Suneido.errlog;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.concurrent.ThreadSafe;

import suneido.SuException;
import suneido.language.Triggers;
import suneido.util.FileUtils;

@ThreadSafe
class Database implements suneido.intfc.database.Database {
	private static final int INT_SIZE = 4;
	final Transactions trans = new Transactions();
	final Storage stor;
	final Object commitLock = new Object();
	final ReentrantReadWriteLock exclusiveLock = new ReentrantReadWriteLock();
	private final Triggers triggers = new Triggers();
	DbHashTrie dbinfo;
	DbHashTrie redirs;
	Tables schema;

	// create

	static Database create(String dbfilename) {
		FileUtils.deleteIfExisting(dbfilename);
		return create(new MmapFile(dbfilename, "rw"));
	}

	static Database create(Storage stor) {
		Database db = new Database(stor, DbHashTrie.empty(stor),
				DbHashTrie.empty(stor), new Tables());
		Bootstrap.create(db);
		return db;
	}

	private Database(Storage stor,
			DbHashTrie dbinfo, DbHashTrie redirs, Tables schema) {
		this.stor = stor;
		this.dbinfo = dbinfo;
		this.redirs = redirs;
		this.schema = schema == null ? SchemaLoader.load(readonlyTran()) : schema;
	}

	// open

	static Database open(String filename) {
		return open(check(filename, "rw"));
	}

	static Database openReadonly(String filename) {
		return open(check(filename, "r"));
	}

	static Database open(Storage stor) {
		Check check = new Check(stor);
		if (! check.fastcheck())
			throw new SuException("database integrity check failed");
		ByteBuffer buf = stor.buffer(-(Tran.TAIL_SIZE + 2 * INT_SIZE));
		int adr = buf.getInt();
		DbHashTrie dbinfo = DbHashTrie.from(stor, adr);
		adr = buf.getInt();
		DbHashTrie redirs = DbHashTrie.from(stor, adr);
		return new Database(stor, dbinfo, redirs);
	}

	/** reopens with same Storage, if MmapFile it is NOT closed and reopened */
	@Override
	public Database reopen() {
		return Database.open(stor);
	}

	private static Storage check(String filename, String mode) {
		Storage stor = new MmapFile(filename, mode);
		Check check = new Check(stor);
		if (! check.fastcheck()) {
			errlog("database not shut down properly last time");
			stor.close();
			DbRebuild.rebuildOrExit(filename);
			stor = new MmapFile(filename, mode);
		}
		return stor;
	}

	private Database(Storage stor, DbHashTrie dbinfo, DbHashTrie redirs) {
		this(stor, dbinfo, redirs, null);
	}

	// used by DbCheck
	Tables schema() {
		return schema;
	}

	@Override
	public ReadTransaction readonlyTran() {
		int num = trans.nextNum(true);
		return new ReadTransaction(num, this);
	}

	@Override
	public UpdateTransaction readwriteTran() {
		int num = trans.nextNum(false);
		return new UpdateTransaction(num, this);
	}

	ExclusiveTransaction exclusiveTran() {
		int num = trans.nextNum(false);
		return new ExclusiveTransaction(num, this);
	}

	@Override
	public TableBuilder createTable(String tableName) {
		checkForSystemTable(tableName, "create");
		return TableBuilder.create(exclusiveTran(), tableName, nextTableNum());
	}

	int nextTableNum() {
		return schema.maxTblNum + 1;
	}

	@Override
	public TableBuilder alterTable(String tableName) {
		checkForSystemTable(tableName, "alter");
		return TableBuilder.alter(exclusiveTran(), tableName);
	}

	@Override
	public TableBuilder ensureTable(String tableName) {
		checkForSystemTable(tableName, "ensure");
		return schema.get(tableName) == null
			? TableBuilder.create(exclusiveTran(), tableName, nextTableNum())
			: TableBuilder.alter(exclusiveTran(), tableName);
	}

	@Override
	public boolean dropTable(String tableName) {
		checkForSystemTable(tableName, "drop");
		ExclusiveTransaction t = exclusiveTran();
		try {
			return TableBuilder.dropTable(t, tableName);
		} finally {
			t.abortIfNotComplete();
		}
	}

	@Override
	public void renameTable(String from, String to) {
		checkForSystemTable(from, "rename");
		ExclusiveTransaction t = exclusiveTran();
		try {
			TableBuilder.renameTable(t, from, to);
		} finally {
			t.abortIfNotComplete();
		}
	}

	@Override
	public void addView(String name, String definition) {
		checkForSystemTable(name, "create view");
		UpdateTransaction t = readwriteTran();
		try {
			if (null != Views.getView(t, name))
				throw new SuException("view: '" + name + "' already exists");
			Views.addView(t, name, definition);
			t.complete();
		} finally {
			t.abortIfNotComplete();
		}
	}

	void dropView(String name) {
		UpdateTransaction t = readwriteTran();
		try {
			Views.dropView(t, name);
		} finally {
			t.abortIfNotComplete();
		}
	}

	static void checkForSystemTable(String tablename, String operation) {
		if (isSystemTable(tablename))
			throw new SuException("can't " + operation +
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
		ReadTransaction t = readonlyTran();
		Table tbl = t.getTable(tableName);
		return tbl == null ? null : tbl.schema(t);
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

}
