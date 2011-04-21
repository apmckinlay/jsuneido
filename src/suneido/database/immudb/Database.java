/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import java.nio.ByteBuffer;

import suneido.database.immudb.schema.Schema;

public class Database {
	static final int INT_SIZE = 4;
	public final Storage stor;

	public Database(Storage stor) {
		this.stor = stor;
	}

	public void open() {
		Check check = new Check(stor);
		if (false == check.fastcheck())
			throw new RuntimeException("database open check failed");
		ByteBuffer buf = stor.buffer(-(Tran.TAIL_SIZE + 2 * INT_SIZE));
		int root = buf.getInt();
		int redirs = buf.getInt();
		new Schema(stor).load(root, redirs);
	}

}
