/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import javax.annotation.concurrent.Immutable;

/**
 * Wrapper for read-only access to dbinfo.
 */
@Immutable
class ReadDbInfo {
	protected final DbHashTrie dbinfo;

	ReadDbInfo(DbHashTrie dbinfo) {
		this.dbinfo = dbinfo;
		assert dbinfo.immutable();
	}

	TableInfo get(int tblnum) {
		return (TableInfo) dbinfo.get(tblnum);
	}

}
