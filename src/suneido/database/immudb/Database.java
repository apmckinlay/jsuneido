/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.ThreadSafe;

import suneido.database.immudb.schema.SchemaLoader;
import suneido.database.immudb.schema.Tables;

@ThreadSafe
public class Database {
	static final int INT_SIZE = 4;
	public final Storage stor;
	final Object commitLock = new Object();
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
		Bootstrap.create(db.updateTran());
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

	public TableBuilder tableBuilder(String tableName) {
		return TableBuilder.builder(updateTran(), tableName, nextTableNum());
	}

	private int nextTableNum() {
		// TODO next table num
		return 4;
	}

	public void close() {
		stor.close();
	}

}
