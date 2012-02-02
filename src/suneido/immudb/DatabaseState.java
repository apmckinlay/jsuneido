/* Copyright 2012 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import javax.annotation.concurrent.Immutable;

@Immutable
public class DatabaseState {
	final DbHashTrie dbinfo;
	final DbHashTrie redirs;
	final Tables schema;

	public DatabaseState(DbHashTrie dbinfo, DbHashTrie redirs, Tables schema) {
		assert dbinfo.immutable();
		assert redirs.immutable();
		this.dbinfo = dbinfo;
		this.redirs = redirs;
		this.schema = schema;
	}

}
