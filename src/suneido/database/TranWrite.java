/* Copyright 2008 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database;

import javax.annotation.concurrent.Immutable;

@Immutable
class TranWrite {
	enum Type { CREATE, DELETE }
	final Type type;
	final int tblnum;
	final long off;

	private TranWrite(Type type, int tblnum, long off) {
		this.type = type;
		this.tblnum = tblnum;
		this.off = off;
	}

	static TranWrite create(int tblnum, long off) {
		return new TranWrite(Type.CREATE, tblnum, off);
	}

	static TranWrite delete(int tblnum, long off) {
		return new TranWrite(Type.DELETE, tblnum, off);
	}
}
