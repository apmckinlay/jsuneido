/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.immudb;

import javax.annotation.concurrent.Immutable;

import suneido.immudb.DbHashTrie.Entry;
import suneido.immudb.DbHashTrie.IntEntry;

@Immutable
class ReadDbInfo {
	protected final Storage stor;
	protected DbHashTrie dbinfo;

	ReadDbInfo(Storage stor, int adr) {
		this.stor = stor;
		dbinfo = DbHashTrie.from(stor, adr);
	}

	ReadDbInfo(Storage stor, DbHashTrie dbinfo) {
		this.stor = stor;
		this.dbinfo = dbinfo;
	}

	DbHashTrie dbinfo() {
		return dbinfo;
	}

	TableInfo get(int tblnum) {
		Entry e = dbinfo.get(tblnum);
		if (e instanceof IntEntry) {
			int adr = ((IntEntry) e).value;
			Record rec = new Record(stor, adr);
			TableInfo ti = new TableInfo(rec, adr);
			dbinfo = dbinfo.with(ti);
			return ti;
		} else
			return (TableInfo) e;
	}

	void check() {
		dbinfo.traverseChanges(checkProc);
	}
	private static final CheckProc checkProc = new CheckProc();
	private static class CheckProc implements DbHashTrie.Process {
		@Override
		public void apply(Entry entry) {
			((TableInfo) entry).check();
		}
	}

}
