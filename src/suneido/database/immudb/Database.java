/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

import suneido.database.immudb.schema.SchemaLoader;
import suneido.database.immudb.schema.Tables;

public class Database {
	static final int INT_SIZE = 4;
	public final Storage stor;
	private int dbinfo;
	private int redirs;
	private Tables schema;

	public Database(Storage stor) {
		this.stor = stor;
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
		dbinfo = buf.getInt();
		redirs = buf.getInt();
		schema = new SchemaLoader(stor).load(dbinfo, redirs);
	}

	public int redirs() {
		return redirs;
	}

	public Tables schema() {
		return schema;
	}

	public Transaction updateTran() {
		return new Transaction(stor, dbinfo, redirs, schema);
	}

	public void close() {
		stor.close();
	}

}
