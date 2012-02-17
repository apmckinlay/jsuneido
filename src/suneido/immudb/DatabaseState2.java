/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import javax.annotation.concurrent.Immutable;

/**
 * Packages dbinfo and schema together so they can be updated atomically.
 */
@Immutable
public class DatabaseState2 {
	final DbHashTrie dbinfo;
	final Tables schema;

	public DatabaseState2(DbHashTrie dbinfo, Tables schema) {
		assert dbinfo.immutable();
		this.dbinfo = dbinfo;
		this.schema = schema;
	}

}
