/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

import suneido.database.immudb.schema.*;

public class Database {
	static final int INT_SIZE = 4;
	public final Storage stor;
	private DbInfo dbinfo;
	private Redirects redirs;
	private Tables schema;

	public Database(Storage stor) {
		this.stor = stor;
	}

	public void create() {
		dbinfo = new DbInfo(stor);
		redirs = new Redirects(DbHashTrie.empty(stor));
		schema = new Tables();
		Bootstrap.create(updateTran());
	}

	public void open() {
		check();
		loadSchema();
	}

	private void check() {
		Check check = new Check(stor);
		if (false == check.fastcheck())
			throw new RuntimeException("database open check failed");
	}

	private void loadSchema() {
		ByteBuffer buf = stor.buffer(-(Tran.TAIL_SIZE + 2 * INT_SIZE));
		int adr = buf.getInt();
		dbinfo = new DbInfo(stor, adr);
		adr = buf.getInt();
		redirs = new Redirects(DbHashTrie.from(stor, adr));
		schema = new SchemaLoader(stor).load(dbinfo, redirs);
	}

	public Tables schema() {
		return schema;
	}

	public Transaction updateTran() {
		return new Transaction(stor, dbinfo, redirs, schema);
	}

	public TableBuilder tableBuilder(String tableName) {
		return TableBuilder.builder(updateTran(), tableName, nextTableNum());
	}

	private int nextTableNum() {
		// TODO
		return 4;
	}

	public void close() {
		stor.close();
	}

}
