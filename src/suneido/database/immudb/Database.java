/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.concurrent.ThreadSafe;

import suneido.database.immudb.schema.Tables;

@ThreadSafe
public class Database {
	static final int INT_SIZE = 4;
	public final Storage stor;
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

	public static Database create(Storage stor) {
		Database db = new Database(stor, DbHashTrie.empty(stor),
				DbHashTrie.empty(stor), new Tables());
		Bootstrap.create(db);
		return db;
	}

	public static Database open(Storage stor) {
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

	public Tables schema() {
		return schema;
	}

	public ReadTransaction readTran() {
		return new ReadTransaction(this);
	}

	public UpdateTransaction updateTran() {
		return new UpdateTransaction(this);
	}

	public ExclusiveTransaction exclusiveTran() {
		return new ExclusiveTransaction(this);
	}

	public TableBuilder createTable(String tableName) {
		return TableBuilder.create(updateTran(), tableName, nextTableNum());
	}

	private int nextTableNum() {
		return schema.maxTblNum + 1;
	}

	public TableBuilder alterTable(String tableName) {
		return TableBuilder.alter(exclusiveTran(), tableName);
	}

	public void close() {
		stor.close();
	}

}
