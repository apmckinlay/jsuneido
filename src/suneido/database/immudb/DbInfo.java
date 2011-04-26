/* Copyright 2011 (c) Suneido Software Corp. All rights reserved.
 * Licensed under GPLv2.
 */

package suneido.database.immudb;

import javax.annotation.concurrent.NotThreadSafe;

import suneido.database.immudb.DbHashTrie.Entry;
import suneido.database.immudb.DbHashTrie.IntEntry;
import suneido.database.immudb.DbHashTrie.Translator;

@NotThreadSafe
public class DbInfo {
	private final Storage stor;
	private DbHashTrie dbinfo;

	public DbInfo(Storage stor) {
		this.stor = stor;
		dbinfo = DbHashTrie.empty(stor);
	}

	public DbInfo(Storage stor, int adr) {
		this.stor = stor;
		dbinfo = DbHashTrie.from(stor, adr);
	}

	public DbInfo(Storage stor, DbHashTrie dbinfo) {
		this.stor = stor;
		this.dbinfo = dbinfo;
	}

	public TableInfo get(int tblnum) {
		Entry e = dbinfo.get(tblnum);
		if (e instanceof IntEntry) {
			int adr = ((IntEntry) e).value;
			Record rec = new Record(stor.buffer(adr));
			TableInfo ti = new TableInfo(rec);
			dbinfo = dbinfo.with(ti);
			return ti;
		} else
			return (TableInfo) e;
	}

	public void add(TableInfo ti) {
		dbinfo = dbinfo.with(ti);
	}

	public int store() {
		return dbinfo.store(new DbInfoTranslator());
	}

	private class DbInfoTranslator implements Translator {
		@Override
		public int translate(Entry e) {
			if (e instanceof TableInfo) {
				Record r = ((TableInfo) e).toRecord();
				return r.store(stor);
			} else
				return ((IntEntry) e).value;
		}
	}

}
