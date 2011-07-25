/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.concurrent.ThreadSafe;

import suneido.SuException;

@ThreadSafe
class Database implements suneido.intfc.database.Database {
	static final int INT_SIZE = 4;
	final Storage stor;
	final Object commitLock = new Object();
	final ReentrantReadWriteLock exclusiveLock = new ReentrantReadWriteLock();
	DbHashTrie dbinfo;
	DbHashTrie redirs;
	Tables schema;

	private Database(Storage stor, DbHashTrie dbinfo, DbHashTrie redirs, Tables schema) {
		this.stor = stor;
		this.dbinfo = dbinfo;
		this.redirs = redirs;
		this.schema = schema;
	}

	static Database create(Storage stor) {
		Database db = new Database(stor, DbHashTrie.empty(stor),
				DbHashTrie.empty(stor), new Tables());
		Bootstrap.create(db);
		return db;
	}

	static Database open(String filename, String mode) {
		return open(new MmapFile(filename, mode));
	}

	static Database open(Storage stor) {
		check(stor);
		ByteBuffer buf = stor.buffer(-(Tran.TAIL_SIZE + 2 * INT_SIZE));
		int adr = buf.getInt();
		DbHashTrie dbinfo = DbHashTrie.from(stor, adr);
		adr = buf.getInt();
		DbHashTrie redirs = DbHashTrie.from(stor, adr);
		Tables schema = SchemaLoader.load(
				new ReadTransaction(stor, dbinfo, null, redirs));
		return new Database(stor, dbinfo, redirs, schema);
	}

	private static void check(Storage stor) {
		Check check = new Check(stor);
		if (false == check.fastcheck())
			throw new RuntimeException("database open check failed");
	}

	Tables schema() {
		return schema;
	}

	@Override
	public ReadTransaction readonlyTran() {
		return new ReadTransaction(this);
	}

	@Override
	public UpdateTransaction readwriteTran() {
		return new UpdateTransaction(this);
	}

	ExclusiveTransaction exclusiveTran() {
		return new ExclusiveTransaction(this);
	}

	TableBuilder createTable(String tableName) {
		return TableBuilder.create(readwriteTran(), tableName, nextTableNum());
	}

	int nextTableNum() {
		return schema.maxTblNum + 1;
	}

	TableBuilder alterTable(String tableName) {
		return TableBuilder.alter(exclusiveTran(), tableName);
	}

	TableBuilder ensureTable2(String tableName) {
		return schema.get(tableName) == null
			? TableBuilder.create(readwriteTran(), tableName, nextTableNum())
			: TableBuilder.alter(exclusiveTran(), tableName);
	}

	@Override
	public boolean removeTable(String tableName) {
		ExclusiveTransaction t = exclusiveTran();
		try {
			return TableBuilder.dropTable(t, tableName);
		} finally {
			t.abortIfNotCommitted();
		}
	}

	@Override
	public void renameTable(String from, String to) {
		ExclusiveTransaction t = exclusiveTran();
		try {
			TableBuilder.renameTable(t, from, to);
		} finally {
			t.abortIfNotCommitted();
		}
	}

	@Override
	public void addView(String name, String definition) {
		UpdateTransaction t = readwriteTran();
		try {
			if (null != Views.getView(t, name))
				throw new SuException("view: '" + name + "' already exists");
			Views.addView(t, name, definition);
		} finally {
			t.abortIfNotCommitted();
		}
	}

	void dropView(String name) {
		UpdateTransaction t = readwriteTran();
		try {
			Views.dropView(t, name);
		} finally {
			t.abortIfNotCommitted();
		}
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
	public void addTable(String tablename) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addColumn(String tablename, String column) {
		ExclusiveTransaction t = exclusiveTran();
		try {
			TableBuilder tb = TableBuilder.alter(t, tablename);
			tb.addColumn(column);
			t.commit();
		} finally {
			t.abortIfNotCommitted();
		}
	}

	@Override
	public void ensureColumn(String tablename, String column) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addIndex(String tablename, String columns, boolean isKey) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addIndex(String tablename, String columns, boolean isKey,
			boolean unique, String fktablename, String fkcolumns, int fkmode) {
		// TODO Auto-generated method stub

	}

	@Override
	public void ensureIndex(String tablename, String columns, boolean isKey,
			boolean unique, String fktablename, String fkcolumns, int fkmode) {
		// TODO Auto-generated method stub

	}

	@Override
	public void renameColumn(String tablename, String oldname, String newname) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeColumn(String tablename, String column) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeIndex(String tablename, String columns) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getSchema(String tablename) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Integer> tranlist() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void limitOutstandingTransactions() {
		// TODO Auto-generated method stub

	}

	@Override
	public int finalSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void force() {
		// TODO Auto-generated method stub

	}

	@Override
	public void disableTrigger(String table) {
		// TODO Auto-generated method stub

	}

	@Override
	public void enableTrigger(String table) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean ensureTable(String tablename) {
		// TODO Auto-generated method stub
		return false;
	}

}
