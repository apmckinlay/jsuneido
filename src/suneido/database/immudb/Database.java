/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

import suneido.database.immudb.schema.*;

public class Database {
	static final int INT_SIZE = 4;
	public final Storage stor;
	DbInfo dbinfo;
	Redirects redirs;
	Tables schema;

	private Database(Storage stor, DbInfo dbinfo, Redirects redirs, Tables schema) {
		this.stor = stor;
		this.dbinfo = dbinfo;
		this.redirs = redirs;
		this.schema = schema;
	}

	public static Database create(Storage stor) {
		Database db = new Database(stor, new DbInfo(stor),
				new Redirects(DbHashTrie.empty(stor)), new Tables());
		Bootstrap.create(db.updateTran());
		return db;
	}

	public static Database open(Storage stor) {
		check(stor);
		ByteBuffer buf = stor.buffer(-(Tran.TAIL_SIZE + 2 * INT_SIZE));
		int adr = buf.getInt();
		DbInfo dbinfo = new DbInfo(stor, adr);
		adr = buf.getInt();
		Redirects redirs = new Redirects(DbHashTrie.from(stor, adr));
		Tables schema = new SchemaLoader(stor).load(dbinfo, redirs);
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

	public Transaction updateTran() {
		return new Transaction(this);
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
